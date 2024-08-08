/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.tasks.executor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.annotations.Delegated
import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.deserializeArgs
import io.infinitic.common.data.methods.encodeReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.DelayedServiceExecutorTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.utils.checkMode
import io.infinitic.common.utils.getMethodPerNameAndParameters
import io.infinitic.common.utils.isDelegated
import io.infinitic.common.utils.withRetry
import io.infinitic.common.utils.withTimeout
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.exceptions.DeferredException
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.task.TaskContextImpl
import io.infinitic.tasks.getMillisBeforeRetry
import io.infinitic.tasks.millis
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.workflowTask.WorkflowTaskImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.TimeoutException

class TaskExecutor(
  private val workerRegistry: WorkerRegistry,
  producerAsync: InfiniticProducerAsync,
  private val client: InfiniticClientInterface
) {

  private val logger = KotlinLogging.logger(this::class.java.name)
  private val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)
  private var withRetry: WithRetry? = null
  private var withTimeout: WithTimeout? = null
  private val emitterName by lazy { EmitterName(producerAsync.producerName) }
  private var isDelegated = false

  @Suppress("UNUSED_PARAMETER")
  suspend fun handle(msg: ServiceExecutorMessage, publishTime: MillisInstant) {
    when (msg) {
      is ExecuteTask -> {
        msg.logDebug { "received $msg" }
        executeTask(msg)
        msg.logTrace { "processed" }
      }
    }
  }

  private suspend fun executeTask(msg: ExecuteTask) = coroutineScope {
    // send taskStarted event
    sendTaskStarted(msg)

    // trying to instantiate the task
    val (service, method, parameters) =
        try {
          parse(msg)
        } catch (e: Exception) {
          // returning the exception (no retry)
          sendTaskFailed(msg, e, msg.taskMeta) { "Unable to parse message $msg" }
          // stop here
          return@coroutineScope
        }

    val taskContext = TaskContextImpl(
        workerName = producer.name,
        workerRegistry = workerRegistry,
        serviceName = msg.serviceName,
        taskId = msg.taskId,
        taskName = msg.methodName,
        workflowId = msg.requester.workflowId,
        workflowName = msg.requester.workflowName,
        workflowVersion = msg.requester.workflowVersion,
        retrySequence = msg.taskRetrySequence,
        retryIndex = msg.taskRetryIndex,
        lastError = msg.lastError,
        tags = msg.taskTags.map { it.tag }.toSet(),
        meta = msg.taskMeta.map.toMutableMap(),
        withRetry = withRetry,
        withTimeout = withTimeout,
        client = client,
    )

    // get local timeout for this task
    val timeout = withTimeout?.millis?.getOrElse {
      // returning the exception (no retry)
      sendTaskFailed(msg, it, taskContext.meta) {
        "Error in ${withTimeout!!::class.java.simpleName} method"
      }
      // stop here
      return@coroutineScope
    } ?: Long.MAX_VALUE

    // task execution
    val output = try {
      //withContext(Dispatchers.Default) {
      withTimeout(timeout) {
        coroutineScope {
          // Put context in execution's thread (it may be used in the following method)
          Task.setContext(taskContext)
          // method execution
          method.invoke(service, *parameters)
        }
      }
      //}
    } catch (e: TimeoutCancellationException) {
      retryTask(msg, taskContext, TimeoutException("Local timeout after $timeout ms"))
      // stop here
      return@coroutineScope
    } catch (e: InvocationTargetException) {
      // exception in method execution
      when (val cause = e.cause ?: e.targetException) {
        // do not retry failed workflow task due to failed/canceled task/workflow
        is DeferredException -> sendTaskFailed(msg, cause, taskContext.meta, null)
        // exception during task execution
        is Exception -> retryTask(msg, taskContext, cause)
        // Throwable are not caught
        else -> throw cause ?: e
      }
      // stop here
      return@coroutineScope
    } catch (e: Exception) {
      // just in case, this should not happen
      sendTaskFailed(msg, e, taskContext.meta) { "Unexpected error" }
      // stop here
      return@coroutineScope
    }

    sendTaskCompleted(msg, output, method, taskContext.meta)
  }

  private suspend fun retryTask(
    msg: ExecuteTask,
    taskContext: TaskContext,
    cause: Exception
  ) {
    // Retrieving delay before retry
    val delayMillis = try {
      msg.logTrace { "retrieving delay before retry" }
      // We set the localThread context here as it may be used in withRetry
      Task.setContext(taskContext)
      // get seconds before retry
      withRetry?.getMillisBeforeRetry(taskContext.retryIndex.toInt(), cause)
    } catch (e: Exception) {
      // We chose here not to obfuscate the initial cause of the failure
      sendTaskFailed(msg, cause, taskContext.meta) {
        "Unable to retry due to an ${e::class.simpleName} error in ${withRetry?.javaClass?.simpleName} method"
      }

      return
    }

    when {
      delayMillis == null || delayMillis <= 0L -> sendTaskFailed(msg, cause, taskContext.meta) {
        cause.message ?: "Unknown error"
      }

      else -> sendRetryTask(msg, cause, MillisDuration(delayMillis), taskContext.meta)
    }
  }

  private suspend fun sendTaskStarted(msg: ExecuteTask) {
    val event = TaskStartedEvent.from(msg, emitterName)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  suspend fun sendTaskFailed(
    msg: ServiceExecutorMessage,
    cause: Throwable,
    meta: Map<String, ByteArray>,
    description: (() -> String)?
  ) {
    if (msg !is ExecuteTask) thisShouldNotHappen()

    description?.let { msg.logError(cause, it) }

    val event = TaskFailedEvent.from(msg, emitterName, cause, meta)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  private suspend fun sendRetryTask(
    msg: ExecuteTask,
    cause: Exception,
    delay: MillisDuration,
    meta: Map<String, ByteArray>
  ) {
    msg.logWarn(cause) { "Retrying in $delay" }

    val executeTask = ExecuteTask.retryFrom(msg, emitterName, cause, meta)
    with(producer) { executeTask.sendTo(DelayedServiceExecutorTopic, delay) }

    // once sent, we publish the event
    val event = TaskRetriedEvent.from(msg, emitterName, cause, delay, meta)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  private suspend fun sendTaskCompleted(
    msg: ExecuteTask,
    output: Any?,
    method: Method,
    meta: Map<String, ByteArray>
  ) {
    if (isDelegated && output != null) {
      msg.logDebug {
        "Method '$method' has an '${Delegated::class.java.name}' annotation, so result is ignored"
      }
    }

    val returnValue = method.encodeReturnValue(output)

    val event = TaskCompletedEvent.from(msg, emitterName, returnValue, isDelegated, meta)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  private fun parse(msg: ExecuteTask): TaskCommand {
    val service = when (msg.isWorkflowTask()) {
      true -> WorkflowTaskImpl()
      false -> workerRegistry.getRegisteredServiceExecutor(msg.serviceName)!!.factory()
    }

    val method = service::class.java.getMethodPerNameAndParameters(
        "${msg.methodName}",
        msg.methodParameterTypes?.types,
        msg.methodParameters.size,
    )

    val args = method.deserializeArgs(msg.methodParameters)

    when (msg.isWorkflowTask()) {
      true -> {
        val workflowTaskParameters = args.first() as WorkflowTaskParameters
        val registered =
            workerRegistry.getRegisteredWorkflowExecutor(msg.requester.workflowName!!)!!
        // workflow instance
        val workflowInstance = registered.getInstance(workflowTaskParameters)
        // method instance
        val workflowMethod = with(workflowTaskParameters) {
          workflowInstance::class.java.getMethodPerNameAndParameters(
              "${workflowMethod.methodName}",
              workflowMethod.methodParameterTypes?.types,
              workflowMethod.methodParameters.size,
          )
        }

        // use withTimeout from registry, if it exists
        this.withTimeout = registered.withTimeout
            // HERE WE ARE LOOKING FOR THE TIMEOUT OF THE WORKFLOW TASK
            // NOT OF THE WORKFLOW ITSELF, THAT'S WHY WE DO NOT LOOK FOR
            // THE @Timeout ANNOTATION OR THE WithTimeout INTERFACE
            // THAT HAS A DIFFERENT MEANING IN WORKFLOWS
            // else use default value
          ?: DEFAULT_WORKFLOW_TASK_TIMEOUT

        // use withRetry from registry, if it exists
        this.withRetry = registered.withRetry
            // else use @Retry annotation, or WithRetry interface
          ?: workflowMethod.withRetry.getOrThrow()
              // else use default value
              ?: DEFAULT_WORKFLOW_TASK_RETRY

        // get checkMode from registry
        val checkMode = registered.checkMode
        // else use CheckMode method annotation on method or class
          ?: workflowMethod.checkMode
          // else use default value
          ?: DEFAULT_WORKFLOW_CHECK_MODE

        with(service as WorkflowTaskImpl) {
          this.checkMode = checkMode
          this.instance = workflowInstance
          this.method = workflowMethod
        }
      }

      false -> {
        val registered = workerRegistry.getRegisteredServiceExecutor(msg.serviceName)!!

        this.withTimeout =
            // use withTimeout from registry, if it exists
            registered.withTimeout
                // else use @Timeout annotation, or WithTimeout interface
              ?: method.withTimeout.getOrThrow()
                  // else use default value
                  ?: DEFAULT_TASK_TIMEOUT

        // use withRetry from registry, if it exists
        this.withRetry = registered.withRetry
            // else use @Timeout annotation, or WithTimeout interface
          ?: method.withRetry.getOrThrow()
              // else use default value
              ?: DEFAULT_TASK_RETRY

        // check is this method has the @Async annotation
        this.isDelegated = method.isDelegated
      }
    }

    return TaskCommand(service, method, args)
  }

  private fun ExecuteTask.logError(e: Throwable, description: () -> String) {
    logger.error(e) {
      "${serviceName}::${methodName} (${taskId}): ${description()}"
    }
  }

  private fun ExecuteTask.logWarn(e: Exception? = null, description: () -> String) {
    logger.warn(e) {
      "${serviceName}::${methodName} (${taskId}): ${description()}"
    }
  }

  private fun ExecuteTask.logDebug(description: () -> String) {
    logger.debug {
      "${serviceName}::${methodName} (${taskId}): ${description()}"
    }
  }

  private fun ExecuteTask.logTrace(description: () -> String) {
    logger.trace {
      "${serviceName}::${methodName} (${taskId}): ${description()}"
    }
  }

  companion object {
    val DEFAULT_TASK_TIMEOUT: WithTimeout? = null
    val DEFAULT_TASK_RETRY: RetryPolicy = RetryPolicy.DEFAULT
    val DEFAULT_WORKFLOW_TASK_TIMEOUT = WithTimeout { 60.0 }
    val DEFAULT_WORKFLOW_TASK_RETRY: RetryPolicy? = null
    val DEFAULT_WORKFLOW_CHECK_MODE = WorkflowCheckMode.simple
  }
}
