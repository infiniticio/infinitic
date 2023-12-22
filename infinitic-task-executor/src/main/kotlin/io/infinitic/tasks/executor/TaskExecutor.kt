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
import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.tasks.executors.errors.TaskFailedError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.utils.getCheckMode
import io.infinitic.common.utils.getWithRetry
import io.infinitic.common.utils.getWithTimeout
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.data.WorkerName
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeoutException
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedWorkflow
import io.infinitic.common.workflows.engine.messages.TaskFailed as TaskFailedWorkflow

class TaskExecutor(
  private val workerRegistry: WorkerRegistry,
  producerAsync: InfiniticProducerAsync,
  private val client: InfiniticClientInterface
) {

  private val logger = KotlinLogging.logger(javaClass.name)
  val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)
  private var withRetry: WithRetry? = null
  private var withTimeout: WithTimeout? = null
  private val emitterName = EmitterName(producerAsync.name)

  suspend fun handle(msg: TaskExecutorMessage) {

    return when (msg) {
      is ExecuteTask -> {
        msg.logDebug { "received $msg" }
        executeTask(msg)
        msg.logTrace { "processed" }
      }
    }
  }

  private suspend fun executeTask(msg: ExecuteTask) = coroutineScope {
    // trying to instantiate the task
    val (service, method, parameters) =
        try {
          parse(msg)
        } catch (e: Exception) {
          // returning the exception (no retry)
          sendTaskFailed(msg, e) { "Unable to parse message $msg" }
          // stop here
          return@coroutineScope
        }

    val taskContext = TaskContextImpl(
        workerName = producer.name,
        workerRegistry = workerRegistry,
        serviceName = msg.serviceName,
        taskId = msg.taskId,
        taskName = msg.methodName,
        workflowId = msg.workflowId,
        workflowName = msg.workflowName,
        workflowVersion = msg.workflowVersion,
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
    val millis = withTimeout?.millis?.getOrElse {
      // returning the exception (no retry)
      sendTaskFailed(msg, it) { "Error in ${withTimeout!!::class.java.simpleName} method" }
      // stop here
      return@coroutineScope
    } ?: Long.MAX_VALUE

    // task execution
    val output = try {
      withTimeout(millis) {
        coroutineScope {
          // context is stored in execution's thread (in case used in method)
          Task.context.set(taskContext)
          // method execution
          method.invoke(service, *parameters)
        }
      }
    } catch (e: TimeoutCancellationException) {
      retryTask(msg, taskContext, TimeoutException("Local timeout after $millis ms"))
      // stop here
      return@coroutineScope
    } catch (e: InvocationTargetException) {
      // exception in method execution
      when (val cause = e.cause) {
        // do not retry failed workflow task due to failed/canceled task/workflow
        is DeferredException -> sendTaskFailed(msg, cause, null)
        // exception during task execution
        is Exception -> retryTask(msg, taskContext, cause)
        // Throwable are not caught
        else -> throw cause ?: e
      }
      // stop here
      return@coroutineScope
    } catch (e: Exception) {
      // just in case, this should not happen
      sendTaskFailed(msg, e) { "Unexpected error" }
      // stop here
      return@coroutineScope
    }

    sendTaskCompleted(msg, output, taskContext.meta)
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
      Task.context.set(taskContext)
      // get seconds before retry
      withRetry?.getMillisBeforeRetry(taskContext.retryIndex.toInt(), cause) ?: 0L
    } catch (e: Exception) {
      // We chose here not to obfuscate the initial cause of the failure
      sendTaskFailed(msg, cause) {
        "Unable to retry due to an ${e::class.simpleName} error in ${withRetry?.javaClass?.simpleName} method"
      }

      return
    }

    when {
      delayMillis <= 0 -> sendTaskFailed(msg, cause) { cause.message ?: "Unknown error" }
      else -> sendRetryTask(msg, cause, MillisDuration(delayMillis), taskContext.meta)
    }
  }

  suspend fun sendTaskFailed(
    msg: TaskExecutorMessage,
    cause: Throwable,
    description: (() -> String)?
  ): Unit = coroutineScope {
    val executionError = cause.getExecutionError()

    when (msg) {
      is ExecuteTask -> {
        description?.let { msg.logError(cause, it) }

        if (msg.clientWaiting) {
          msg.logTrace { "sending TaskFailed to client" }

          val taskFailed = TaskFailedClient(
              recipientName = ClientName.from(msg.emitterName),
              taskId = msg.taskId,
              cause = executionError,
              emitterName = emitterName,
          )
          launch { producer.sendToClient(taskFailed) }
        }

        if (msg.workflowId != null) {
          msg.logTrace { "sending TaskFailed to workflow engine" }

          val taskFailed = TaskFailedWorkflow(
              workflowName = msg.workflowName ?: thisShouldNotHappen(),
              workflowId = msg.workflowId ?: thisShouldNotHappen(),
              methodRunId = msg.methodRunId ?: thisShouldNotHappen(),
              taskFailedError = TaskFailedError(
                  serviceName = msg.serviceName,
                  methodName = msg.methodName,
                  taskId = msg.taskId,
                  cause = executionError,
              ),
              deferredError = getDeferredError(cause),
              emitterName = emitterName,
          )
          launch { producer.sendToWorkflowEngineLater(taskFailed) }
        }
      }
    }
  }

  private suspend fun sendRetryTask(
    msg: ExecuteTask,
    cause: Exception,
    delay: MillisDuration,
    meta: Map<String, ByteArray>
  ) {
    msg.logWarn(cause) { "Retrying in $delay" }

    val executeTask = msg.copy(
        taskRetryIndex = msg.taskRetryIndex + 1,
        lastError = cause.getExecutionError(),
        taskMeta = TaskMeta(meta),
    )

    producer.sendToTaskExecutor(executeTask, delay)
  }

  private suspend fun sendTaskCompleted(
    msg: ExecuteTask,
    value: Any?,
    meta: Map<String, ByteArray>
  ) = coroutineScope {
    val taskMeta = TaskMeta(meta)
    val returnValue = ReturnValue.from(value)

    if (msg.clientWaiting) {
      msg.logTrace { "sending TaskCompleted to client" }

      val taskCompleted = TaskCompletedClient(
          recipientName = ClientName.from(msg.emitterName),
          taskId = msg.taskId,
          taskReturnValue = returnValue,
          taskMeta = taskMeta,
          emitterName = emitterName,
      )

      launch { producer.sendToClient(taskCompleted) }
    }

    if (msg.workflowId != null) {
      msg.logTrace { "sending TaskCompleted to workflow engine" }

      val taskCompleted = TaskCompletedWorkflow(
          workflowName = msg.workflowName ?: thisShouldNotHappen(),
          workflowId = msg.workflowId ?: thisShouldNotHappen(),
          methodRunId = msg.methodRunId ?: thisShouldNotHappen(),
          taskReturnValue =
          TaskReturnValue(
              serviceName = msg.serviceName,
              taskId = msg.taskId,
              taskMeta = taskMeta,
              returnValue = returnValue,
          ),
          emitterName = emitterName,
      )

      launch { producer.sendToWorkflowEngineLater(taskCompleted) }
    }

    launch { sendRemoveTags(msg) }
  }

  private suspend fun sendRemoveTags(msg: ExecuteTask) = coroutineScope {
    msg.logTrace { "sending removeTagFromTask to task tags" }

    msg.taskTags.map {
      val removeTagFromTask = RemoveTagFromTask(
          taskTag = it,
          serviceName = msg.serviceName,
          taskId = msg.taskId,
          emitterName = emitterName,
      )
      launch { producer.sendToTaskTag(removeTagFromTask) }
    }
  }

  private fun parse(msg: ExecuteTask): TaskCommand {
    val service = when (msg.isWorkflowTask()) {
      true -> WorkflowTaskImpl()
      false -> workerRegistry.getRegisteredService(msg.serviceName).factory()
    }

    val taskMethod = getMethodPerNameAndParameters(
        service::class.java,
        "${msg.methodName}",
        msg.methodParameterTypes?.types,
        msg.methodParameters.size,
    )

    val parameters = msg.methodParameters.map { it.deserialize() }.toTypedArray()

    when (msg.isWorkflowTask()) {
      true -> {
        val workflowTaskParameters = parameters.first() as WorkflowTaskParameters
        val registered = workerRegistry.getRegisteredWorkflow(msg.workflowName!!)
        val workflow = registered.getInstance(workflowTaskParameters.workflowVersion)
        val workflowMethod = with(workflowTaskParameters) {
          // method instance
          getMethodPerNameAndParameters(
              workflow::class.java,
              "${methodRun.methodName}",
              methodRun.methodParameterTypes?.types,
              methodRun.methodParameters.size,
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
          ?: workflowMethod.getWithRetry().getOrThrow()
              // else use default value
              ?: DEFAULT_WORKFLOW_TASK_RETRY

        // get checkMode from registry
        val checkMode = registered.checkMode
        // else use CheckMode method annotation on method or class
          ?: workflowMethod.getCheckMode()
          // else use default value
          ?: DEFAULT_WORKFLOW_CHECK_MODE

        with(service as WorkflowTaskImpl) {
          this.checkMode = checkMode
          this.instance = workflow
          this.method = workflowMethod
        }
      }

      false -> {
        val registered = workerRegistry.getRegisteredService(msg.serviceName)

        this.withTimeout =
            // use withTimeout from registry, if it exists
            registered.withTimeout
                // else use @Timeout annotation, or WithTimeout interface
              ?: taskMethod.getWithTimeout().getOrThrow()
                  // else use default value
                  ?: DEFAULT_TASK_TIMEOUT

        // use withRetry from registry, if it exists
        this.withRetry = registered.withRetry
            // else use @Timeout annotation, or WithTimeout interface
          ?: taskMethod.getWithRetry().getOrThrow()
              // else use default value
              ?: DEFAULT_TASK_RETRY
      }
    }

    return TaskCommand(service, taskMethod, parameters)
  }

  private fun getDeferredError(e: Throwable) =
      when (e is DeferredException) {
        true -> DeferredError.from(e)
        false -> null
      }

  private fun Throwable.getExecutionError() =
      ExecutionError.from(WorkerName.from(emitterName), this)

  private fun ExecuteTask.logError(e: Throwable, description: () -> String) {
    logger.error(e) {
      "${serviceName}::${methodName} (${taskId}): ${description()}"
    }
  }

  private fun ExecuteTask.logWarn(e: Exception, description: () -> String) {
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
    val DEFAULT_WORKFLOW_TASK_TIMEOUT = WithTimeout { 60.0 } // 1 minute
    val DEFAULT_TASK_TIMEOUT = null
    val DEFAULT_TASK_RETRY = RetryPolicy.DEFAULT
    val DEFAULT_WORKFLOW_TASK_RETRY = null
    val DEFAULT_WORKFLOW_CHECK_MODE = WorkflowCheckMode.simple
  }
}
