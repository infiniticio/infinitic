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
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.deserializeArgs
import io.infinitic.common.data.methods.encodeReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.utils.checkMode
import io.infinitic.common.utils.getMethodPerNameAndParameters
import io.infinitic.common.utils.isDelegated
import io.infinitic.common.utils.withRetry
import io.infinitic.common.utils.withTimeout
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.DeferredException
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
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
import kotlin.reflect.jvm.javaMethod

class TaskExecutor(
  private val workerRegistry: WorkerRegistry,
  producerAsync: InfiniticProducerAsync,
  private val client: InfiniticClientInterface
) {

  private val logger = KotlinLogging.logger(this::class.java.name)
  private val producer = LoggedInfiniticProducer(this::class.java.name, producerAsync)
  private val emitterName by lazy { EmitterName(producerAsync.producerName) }
  private var withRetry: WithRetry? = null
  private var withTimeout: WithTimeout? = null
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
    msg.sendTaskStarted()

    // Retrieve service instance, service method and method arguments
    val (service, method, args) =
        try {
          msg.parse()
        } catch (e: Exception) {
          // returning the exception (no retry)
          msg.sendTaskFailed(e, msg.taskMeta) { "Unable to parse message $msg" }
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
      msg.sendTaskFailed(it, taskContext.meta) {
        "Error in ${withTimeout!!::class.java.simpleName} method"
      }
      // stop here
      return@coroutineScope
    } ?: Long.MAX_VALUE

    // task execution
    val output = try {
      withTimeout(timeout) {
        coroutineScope {
          // Put context in execution's thread (it may be used in the method)
          Task.setContext(taskContext)
          // method execution
          method.invoke(service, *args)
        }
      }
    } catch (e: TimeoutCancellationException) {
      msg.retryOrSendTaskFailed(
          taskContext,
          TimeoutException("Task execution timed-out after $timeout ms"),
      )
      // stop here
      return@coroutineScope
    } catch (e: InvocationTargetException) {
      // exception in method execution
      when (val cause = e.cause ?: e.targetException) {
        // Do not retry failed workflow task due to failed/canceled task/workflow
        is DeferredException -> msg.sendTaskFailed(cause, taskContext.meta) { cause.description }
        // Check if this task should be retried
        is Exception -> msg.retryOrSendTaskFailed(taskContext, cause)
        // Throwable are not caught
        is Throwable -> throw cause
      }
      // stop here
      return@coroutineScope
    } catch (e: Exception) {
      // Catch everything else
      msg.sendTaskFailed(e, taskContext.meta)
      // stop here
      return@coroutineScope
    }

    msg.sendTaskCompleted(output, method, taskContext.meta)
  }

  private suspend fun ExecuteTask.retryOrSendTaskFailed(
    taskContext: TaskContext,
    cause: Exception
  ) {
    val delayMillis = getDelayMillis(taskContext, cause).getOrElse {
      // We chose here not to obfuscate the initial cause of the failure
      sendTaskFailed(cause, taskContext.meta) {
        "Unable to retry. A ${it::class.java.name} has threw in $withRetry method:\n" + it.stackTraceToString()
      }
      return
    }

    when (delayMillis) {
      null -> sendTaskFailed(cause, taskContext.meta)
      else -> sendRetryTask(cause, MillisDuration(delayMillis), taskContext.meta)
    }
  }

  private fun ExecuteTask.getDelayMillis(taskContext: TaskContext, cause: Exception) = try {
    logDebug { "Retrieving delay before retry" }
    // We set the localThread context here as it may be used in withRetry
    Task.setContext(taskContext)
    // get millis before retry
    val delayMillis = withRetry?.getMillisBeforeRetry(taskContext.retryIndex.toInt(), cause)
    logTrace { "Delay before retry retrieved: $delayMillis" }
    Result.success(delayMillis)
  } catch (e: Exception) {
    Result.failure(e)
  }

  private suspend fun ExecuteTask.sendTaskStarted() {
    val event = TaskStartedEvent.from(this, this@TaskExecutor.emitterName)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  suspend fun ExecuteTask.sendTaskFailed(
    cause: Throwable,
    meta: Map<String, ByteArray>,
    description: (() -> String)? = null
  ) {
    logError(cause, description)
    val event = TaskFailedEvent.from(this, this@TaskExecutor.emitterName, cause, meta)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  private suspend fun ExecuteTask.sendRetryTask(
    cause: Exception,
    delay: MillisDuration,
    meta: Map<String, ByteArray>
  ) {
    logWarn(cause) { "Retrying in $delay" }
    val emitterName = this@TaskExecutor.emitterName

    val executeTask = ExecuteTask.retryFrom(this, emitterName, cause, meta)
    with(producer) { executeTask.sendTo(RetryServiceExecutorTopic, delay) }

    // once sent, we publish the event
    val event = TaskRetriedEvent.from(this, emitterName, cause, delay, meta)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  private suspend fun ExecuteTask.sendTaskCompleted(
    output: Any?,
    method: Method,
    meta: Map<String, ByteArray>
  ) {
    if (isDelegated && output != null) logDebug {
      "Method '$method' has an '${Delegated::class.java.name}' annotation, so its result is ignored"
    }
    val returnValue = method.encodeReturnValue(output)
    val event =
        TaskCompletedEvent.from(this, this@TaskExecutor.emitterName, returnValue, isDelegated, meta)
    with(producer) { event.sendTo(ServiceEventsTopic) }
  }

  private fun ExecuteTask.parse(): Triple<Any, Method, Array<*>> =
      when (isWorkflowTask()) {
        true -> parseWorkflowTask((requester as WorkflowRequester).workflowName, methodParameters)
        false -> parseTask(serviceName, methodName, methodParameterTypes, methodParameters)
      }

  private fun parseTask(
    serviceName: ServiceName,
    methodName: MethodName,
    methodParameterTypes: MethodParameterTypes?,
    methodArgs: MethodArgs
  ): Triple<Any, Method, Array<*>> {
    val registeredServiceExecutor = workerRegistry.serviceExecutors[serviceName]!!

    val serviceInstance = registeredServiceExecutor.factory()
    val serviceMethod = serviceInstance::class.java.getMethodPerNameAndParameters(
        "$methodName",
        methodParameterTypes?.types,
        methodArgs.size,
    )
    val serviceArgs = serviceMethod.deserializeArgs(methodArgs)

    this.withTimeout =
        // use withTimeout from registry, if it exists
        registeredServiceExecutor.withTimeout
            // else use @Timeout annotation, or WithTimeout interface
          ?: serviceMethod.withTimeout.getOrThrow()
              // else use default value
              ?: TASK_WITH_TIMEOUT_DEFAULT

    // use withRetry from registry, if it exists
    this.withRetry = registeredServiceExecutor.withRetry
        // else use @Timeout annotation, or WithTimeout interface
      ?: serviceMethod.withRetry.getOrThrow()
          // else use default value
          ?: TASK_WITH_RETRY_DEFAULT

    // check is this method has the @Async annotation
    this.isDelegated = serviceMethod.isDelegated

    return Triple(serviceInstance, serviceMethod, serviceArgs)
  }

  private fun parseWorkflowTask(
    workflowName: WorkflowName,
    methodArgs: MethodArgs
  ): Triple<Any, Method, Array<*>> {
    val serviceInstance = WorkflowTaskImpl()
    val serviceMethod = WorkflowTaskImpl::handle.javaMethod!!
    val serviceArgs = serviceMethod.deserializeArgs(methodArgs)

    val workflowTaskParameters = serviceArgs[0] as WorkflowTaskParameters

    // workflow registered in worker
    val registeredWorkflowExecutor = workerRegistry.workflowExecutors[workflowName]!!
    // workflow instance
    val workflowInstance = registeredWorkflowExecutor.getInstance(workflowTaskParameters)
    // method of the workflow instance
    val workflowMethod = with(workflowTaskParameters) {
      workflowInstance::class.java.getMethodPerNameAndParameters(
          "${workflowMethod.methodName}",
          workflowMethod.methodParameterTypes?.types,
          workflowMethod.methodParameters.size,
      )
    }

    // get checkMode from registry
    val checkMode = registeredWorkflowExecutor.checkMode
    // else use CheckMode method annotation on method or class
      ?: workflowMethod.checkMode
      // else use default value
      ?: WORKFLOW_CHECK_MODE_DEFAULT

    serviceInstance.apply {
      this.checkMode = checkMode
      this.instance = workflowInstance
      this.method = workflowMethod
    }

    // use withTimeout from registry, if it exists
    this.withTimeout = registeredWorkflowExecutor.withTimeout
        // HERE WE ARE LOOKING FOR THE TIMEOUT OF THE WORKFLOW TASK
        // NOT OF THE WORKFLOW ITSELF, THAT'S WHY WE DO NOT LOOK FOR
        // THE @Timeout ANNOTATION OR THE WithTimeout INTERFACE
        // THAT HAS A DIFFERENT MEANING IN WORKFLOWS
        // else use default value
      ?: WORKFLOW_TASK_WITH_TIMEOUT_DEFAULT

    // use withRetry from registry, if it exists
    this.withRetry = registeredWorkflowExecutor.withRetry
        // else use @Retry annotation, or WithRetry interface
      ?: workflowMethod.withRetry.getOrThrow()
          // else use default value
          ?: WORKFLOW_TASK_WITH_RETRY_DEFAULT

    return Triple(serviceInstance, serviceMethod, serviceArgs)
  }

  private fun ExecuteTask.logError(e: Throwable, description: (() -> String)?) {
    logger.error(e) {
      "${serviceName}::${methodName} (${taskId}): ${description?.let { it() } ?: e.message ?: ""}"
    }
  }

  private fun ExecuteTask.logWarn(e: Exception, description: (() -> String)?) {
    logger.warn(e) {
      "${serviceName}::${methodName} (${taskId}): ${description?.let { it() } ?: e.message ?: ""}"
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
    val TASK_WITH_TIMEOUT_DEFAULT: WithTimeout? = null
    val TASK_WITH_RETRY_DEFAULT: WithRetry? = null
    val WORKFLOW_TASK_WITH_TIMEOUT_DEFAULT = WithTimeout { 60.0 }
    val WORKFLOW_TASK_WITH_RETRY_DEFAULT: WithRetry? = null
    val WORKFLOW_CHECK_MODE_DEFAULT = WorkflowCheckMode.simple
  }
}
