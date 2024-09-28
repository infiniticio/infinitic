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
import io.infinitic.common.registry.ExecutorRegistryInterface
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
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.MessageBatchConfig
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.utils.checkMode
import io.infinitic.common.utils.getBatchConfig
import io.infinitic.common.utils.getBatchMethod
import io.infinitic.common.utils.getMethodPerNameAndParameters
import io.infinitic.common.utils.isDelegated
import io.infinitic.common.utils.withRetry
import io.infinitic.common.utils.withTimeout
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.TimeoutException
import kotlin.reflect.jvm.javaMethod

class TaskExecutor(
  private val registry: ExecutorRegistryInterface,
  private val producer: InfiniticProducer,
  private val client: InfiniticClientInterface
) {
  private suspend fun getEmitterName() = EmitterName(producer.getName())

  @Suppress("UNUSED_PARAMETER")
  suspend fun handle(msg: ServiceExecutorMessage, publishTime: MillisInstant) {
    when (msg) {
      is ExecuteTask -> msg.process()
    }
  }

  suspend fun handleBatch(messages: List<ServiceExecutorMessage>) = coroutineScope {
    val executeTasks = messages.map {
      when (it) {
        is ExecuteTask -> it
      }
    }
    executeTasks.process()
  }

  suspend fun assessBatching(msg: ServiceExecutorMessage): Result<MessageBatchConfig?> =
      when (msg) {
        is ExecuteTask -> msg.assessBatching()
      }

  private suspend fun ExecuteTask.assessBatching(): Result<MessageBatchConfig?> = try {
    Result.success(getMethod().getBatchConfig())
  } catch (e: Exception) {
    sendTaskFailed(e, taskMeta) { "Error when retrieving the batching config for $this" }
    Result.failure(e)
  }

  private data class TaskData(
    val instance: Any,
    val method: Method,
    val withTimeout: WithTimeout?,
    val withRetry: WithRetry?,
    val isDelegated: Boolean,
    val args: List<*>,
    val context: TaskContext
  )

  private data class BatchData(
    val instance: Any,
    val method: Method,
    val withTimeout: WithTimeout?,
    val withRetry: WithRetry?,
    val isDelegated: Boolean,
    val argsList: List<List<*>>,
    val contextList: List<TaskContext>
  )

  private suspend fun List<ExecuteTask>.process() = coroutineScope {
    // Signal that the tasks have started
    sendTaskStarted()

    // Parse the batch. If parsing fails, return without proceeding
    val batchData = parseBatch().getOrElse { return@coroutineScope }

    // Get the batch timeout. If this operation fails, return without proceeding
    val timeout = getBatchTimeout(batchData).getOrElse { return@coroutineScope }

    // Execute the batch with the specified timeout. If this operation fails, return without proceeding
    val output = executeWithTimeout(batchData, timeout).getOrElse { return@coroutineScope }

    // All tasks have completed successfully
    sendTaskCompleted(output, batchData)

  }

  private suspend fun ExecuteTask.process() = coroutineScope {
    // Signal that the task has started
    sendTaskStarted()

    // Parse the task data. If parsing fails, return without proceeding
    val taskData = parseTask().getOrElse { return@coroutineScope }

    // Get the task timeout. If this operation fails, return without proceeding
    val timeout = getTaskTimeout(taskData).getOrElse { return@coroutineScope }

    // Execute the task with the specified timeout. If this operation fails, return without proceeding
    val output = executeWithTimeout(taskData, timeout).getOrElse { return@coroutineScope }

    // Signal that the task has completed successfully
    sendTaskCompleted(output, taskData)
  }

  private suspend fun List<ExecuteTask>.sendTaskStarted() = coroutineScope {
    map { executeTask ->
      launch {
        executeTask.sendTaskStarted()
      }
    }
  }

  private suspend fun ExecuteTask.sendTaskStarted() {
    val event = TaskStartedEvent.from(this, getEmitterName())
    with(producer) { event.sendTo(ServiceExecutorEventTopic) }
  }

  private suspend fun List<ExecuteTask>.parseBatch(): Result<BatchData> {
    return try {
      parseBatchData().let { Result.success(it) }
    } catch (e: Exception) {
      sendTaskFailed(e, map { it.taskMeta }) { "Unable to parse batch of messages" }
      Result.failure(e)
    }
  }

  private suspend fun ExecuteTask.parseTask(): Result<TaskData> {
    return try {
      when (isWorkflowTask()) {
        true -> parseWorkflowData()
        false -> parseTaskData()
      }.let { Result.success(it) }
    } catch (e: Exception) {
      sendTaskFailed(e, taskMeta) { "Unable to parse message $this" }
      Result.failure(e)
    }
  }

  private suspend fun getTimeout(
    withTimeout: WithTimeout?,
    onError: suspend (Throwable) -> Unit
  ): Result<Long> {
    val timeoutMillis = withTimeout?.millis
    val timeoutException = timeoutMillis?.exceptionOrNull()

    if (timeoutException != null) {
      onError(timeoutException)
      return Result.failure(timeoutException)
    }

    val timeoutValue = timeoutMillis?.getOrNull() ?: Long.MAX_VALUE
    return Result.success(timeoutValue)
  }

  private suspend fun List<ExecuteTask>.getBatchTimeout(batchData: BatchData): Result<Long> =
      getTimeout(batchData.withTimeout) { e ->
        sendTaskFailed(
            e,
            map { it.taskMeta },
        ) { "Error in ${WithTimeout::getTimeoutSeconds.name} method" }
      }

  private suspend fun ExecuteTask.getTaskTimeout(taskData: TaskData): Result<Long> =
      getTimeout(taskData.withTimeout) {
        sendTaskFailed(it, taskMeta) { "Error in ${WithTimeout::getTimeoutSeconds.name} method" }
      }

  private suspend fun List<ExecuteTask>.executeWithTimeout(
    batchData: BatchData,
    timeout: Long
  ): Result<List<Any?>> {
    return try {
      withTimeout(timeout) {
        coroutineScope {
          Task.setBatchContext(batchData.contextList)
          Result.success(
              batchData.method.invoke(batchData.instance, batchData.argsList) as List<Any?>,
          )
        }
      }
    } catch (e: TimeoutCancellationException) {
      handleTimeoutException(batchData, timeout)
      Result.failure(e)
    } catch (e: InvocationTargetException) {
      handleInvocationTargetException(batchData, e)
      Result.failure(e)
    } catch (e: Exception) {
      sendTaskFailed(e, Task.batchContext.map { it.meta }) {
        "An error occurred while processing batch messages"
      }
      Result.failure(e)
    }
  }

  private suspend fun ExecuteTask.executeWithTimeout(
    taskData: TaskData,
    timeout: Long
  ): Result<Any?> {
    return try {
      withTimeout(timeout) {
        coroutineScope {
          Task.setContext(taskData.context)
          Result.success(taskData.method.invoke(taskData.instance, *taskData.args.toTypedArray()))
        }
      }
    } catch (e: TimeoutCancellationException) {
      handleTimeoutException(taskData, timeout)
      Result.failure(e)
    } catch (e: InvocationTargetException) {
      handleInvocationTargetException(taskData, e)
      Result.failure(e)
    } catch (e: Exception) {
      sendTaskFailed(e, taskData.context.meta)
      Result.failure(e)
    }
  }

  private suspend fun List<ExecuteTask>.handleTimeoutException(
    batchData: BatchData,
    timeout: Long
  ) {
    retryOrSendTaskFailed(
        batchData.withRetry,
        batchData.contextList,
        TimeoutException("Batch execution timed-out after $timeout ms"),
    )
  }

  private suspend fun ExecuteTask.handleTimeoutException(taskData: TaskData, timeout: Long) {
    retryOrSendTaskFailed(
        taskData.withRetry,
        taskData.context,
        TimeoutException("Task execution timed-out after $timeout ms"),
    )
  }

  private suspend fun List<ExecuteTask>.handleInvocationTargetException(
    batchData: BatchData,
    e: InvocationTargetException
  ) {
    val cause = e.cause ?: e.targetException
    when (cause) {
      is Exception -> retryOrSendTaskFailed(batchData.withRetry, batchData.contextList, cause)
      is Throwable -> throw cause
    }
  }

  private suspend fun ExecuteTask.handleInvocationTargetException(
    taskData: TaskData,
    e: InvocationTargetException
  ) {
    val cause = e.cause ?: e.targetException
    when (cause) {
      is DeferredException -> sendTaskFailed(cause, taskData.context.meta) { cause.description }
      is Exception -> retryOrSendTaskFailed(taskData.withRetry, taskData.context, cause)
      is Throwable -> throw cause
    }
  }

  private suspend fun List<ExecuteTask>.retryOrSendTaskFailed(
    withRetry: WithRetry?,
    batchContext: List<TaskContext>,
    cause: Exception
  ) {
    val metas = batchContext.map { it.meta }
    val delaysMillis = getDelayMillis(withRetry, batchContext, cause)

    delaysMillis.forEachIndexed { index, result ->
      val executeTask = this[index]
      val meta = metas[index]

      result.onFailure { e ->
        executeTask.sendTaskFailed(cause, meta) {
          "Unable to retry. An exception (${e::class.java.name}) occurred in " +
              "${withRetry!!::getSecondsBeforeRetry.name} method:\n${e.stackTraceToString()}"
        }
      }

      result.onSuccess { delayMillis ->
        when (delayMillis) {
          null -> executeTask.sendTaskFailed(cause, meta) { "Unable to process batch messages" }
          else -> executeTask.sendRetryTask(cause, MillisDuration(delayMillis), meta)
        }
      }
    }
  }

  private suspend fun ExecuteTask.retryOrSendTaskFailed(
    withRetry: WithRetry?,
    taskContext: TaskContext,
    cause: Exception
  ) {
    val delayResult = getDelayMillis(withRetry, taskContext, cause)

    delayResult.onFailure { e ->
      sendTaskFailed(cause, taskContext.meta) {
        "Unable to retry. A ${e::class.java.name} has threw in $withRetry method:\n" + e.stackTraceToString()
      }
    }

    delayResult.onSuccess { delayMillis ->
      when (delayMillis) {
        null -> sendTaskFailed(cause, taskContext.meta)
        else -> sendRetryTask(cause, MillisDuration(delayMillis), taskContext.meta)
      }
    }
  }

  private suspend fun List<ExecuteTask>.getDelayMillis(
    withRetry: WithRetry?,
    taskContexts: List<TaskContext>,
    cause: Exception
  ): List<Result<Long?>> =
      coroutineScope {
        mapIndexed { index, executeTask ->
          async {
            executeTask.getDelayMillis(withRetry, taskContexts[index], cause)
          }
        }.toList().awaitAll()
      }

  private fun ExecuteTask.getDelayMillis(
    withRetry: WithRetry?,
    taskContext: TaskContext,
    cause: Exception
  ): Result<Long?> = try {
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

  private suspend fun List<ExecuteTask>.sendTaskFailed(
    cause: Throwable,
    meta: List<Map<String, ByteArray>>,
    description: (() -> String)?
  ) = coroutineScope {
    mapIndexed { index, executeTask ->
      launch {
        executeTask.sendTaskFailed(cause, meta[index], description)
      }
    }
  }

  suspend fun ExecuteTask.sendTaskFailed(
    cause: Throwable,
    meta: Map<String, ByteArray>,
    description: (() -> String)? = null
  ) {
    logError(cause, description)
    val event = TaskFailedEvent.from(this, getEmitterName(), cause, meta)
    with(producer) { event.sendTo(ServiceExecutorEventTopic) }
  }

  private suspend fun ExecuteTask.sendRetryTask(
    cause: Exception,
    delay: MillisDuration,
    meta: Map<String, ByteArray>
  ) {
    logWarn(cause) { "Retrying in $delay" }

    val executeTask = ExecuteTask.retryFrom(this, getEmitterName(), cause, meta)
    with(producer) { executeTask.sendTo(ServiceExecutorRetryTopic, delay) }

    // once sent, we publish the event
    val event = TaskRetriedEvent.from(this, getEmitterName(), cause, delay, meta)
    with(producer) { event.sendTo(ServiceExecutorEventTopic) }
  }

  private suspend fun List<ExecuteTask>.sendTaskCompleted(
    output: List<Any?>,
    batchData: BatchData
  ) = coroutineScope {
    mapIndexed { i, executeTask ->
      launch {
        executeTask.sendTaskCompleted(
            output[i],
            batchData.isDelegated,
            batchData.method,
            batchData.contextList[i].meta,
        )
      }
    }
  }

  private suspend fun ExecuteTask.sendTaskCompleted(
    output: Any?,
    isDelegated: Boolean,
    method: Method,
    meta: Map<String, ByteArray>
  ) {
    if (isDelegated && output != null) logDebug {
      "Method '${method}' has an '${Delegated::class.java.name}' annotation, so its result is ignored"
    }
    val returnValue = method.encodeReturnValue(output)
    val taskCompletedEvent = TaskCompletedEvent.from(
        this,
        getEmitterName(),
        returnValue,
        isDelegated,
        meta,
    )
    with(producer) { taskCompletedEvent.sendTo(ServiceExecutorEventTopic) }
  }

  private suspend fun ExecuteTask.sendTaskCompleted(output: Any?, taskData: TaskData) =
      sendTaskCompleted(output, taskData.isDelegated, taskData.method, taskData.context.meta)

  private fun ExecuteTask.getMethod(): Method {
    // Obtain the service class instance from the registry
    val klass = registry.getServiceExecutorInstance(serviceName)::class.java

    // Return the method corresponding to the specified name and parameter types
    return klass.getMethodPerNameAndParameters(
        "$methodName",
        methodParameterTypes?.types,
        methodArgs.size,
    )
  }

  private suspend fun ExecuteTask.getContext(
    withRetry: WithRetry?,
    withTimeout: WithTimeout?
  ): TaskContext = TaskContextImpl(
      workerName = producer.getName(),
      serviceName = serviceName,
      taskId = taskId,
      taskName = methodName,
      workflowId = requester.workflowId,
      workflowName = requester.workflowName,
      workflowVersion = requester.workflowVersion,
      retrySequence = taskRetrySequence,
      retryIndex = taskRetryIndex,
      lastError = lastError,
      tags = taskTags.map { it.tag }.toSet(),
      meta = taskMeta.map.toMutableMap(),
      withRetry = withRetry,
      withTimeout = withTimeout,
      client = client,
  )

  private suspend fun List<ExecuteTask>.parseBatchData(): BatchData {
    // Select the first task or throw an exception if the list is empty
    val executeTask = firstOrNull() ?: thisShouldNotHappen()

    // Parse the task data
    val taskData = executeTask.parseBatch()

    // Deserialize the method arguments for each task, asynchronously as it can be expensive
    val argsList = coroutineScope {
      map { async { taskData.method.deserializeArgs(it.methodArgs) } }.toList().awaitAll()
    }

    // Retrieve the context for each task asynchronously
    val contextList = map { it.getContext(taskData.withRetry, taskData.withTimeout) }

    // Return BatchTaskData containing the task information and contexts
    return BatchData(
        instance = taskData.instance,
        method = taskData.method,
        withTimeout = taskData.withTimeout,
        withRetry = taskData.withRetry,
        isDelegated = taskData.isDelegated,
        argsList = argsList,
        contextList = contextList,
    )
  }

  private suspend fun ExecuteTask.parseBatch(): TaskData {
    val serviceInstance = registry.getServiceExecutorInstance(serviceName)

    val serviceMethod = serviceInstance::class.java.getMethodPerNameAndParameters(
        "$methodName",
        methodParameterTypes?.types,
        methodArgs.size,
    )

    val batchMethod = serviceMethod.getBatchMethod() ?: thisShouldNotHappen()

    val withTimeout = getWithTimeout(serviceName, batchMethod)

    val withRetry = getWithRetry(serviceName, batchMethod)

    val isDelegated = batchMethod.isDelegated

    val taskContext = getContext(withRetry, withTimeout)

    return TaskData(
        serviceInstance,
        serviceMethod,
        withTimeout,
        withRetry,
        isDelegated,
        listOf<Unit>(),
        taskContext,
    )
  }

  private suspend fun ExecuteTask.parseTaskData(): TaskData {
    val serviceInstance = registry.getServiceExecutorInstance(serviceName)

    val serviceMethod = serviceInstance::class.java.getMethodPerNameAndParameters(
        "$methodName",
        methodParameterTypes?.types,
        methodArgs.size,
    )
    val serviceArgs = serviceMethod.deserializeArgs(methodArgs)

    val withTimeout = getWithTimeout(serviceName, serviceMethod)

    val withRetry = getWithRetry(serviceName, serviceMethod)

    // check is this method has the @Async annotation
    val isDelegated = serviceMethod.isDelegated

    val taskContext = getContext(withRetry, withTimeout)

    return TaskData(
        serviceInstance,
        serviceMethod,
        withTimeout,
        withRetry,
        isDelegated,
        serviceArgs,
        taskContext,
    )
  }

  private fun getWithTimeout(serviceName: ServiceName, serviceMethod: Method): WithTimeout? =
      // use withTimeout from registry, if it exists
      when (val wt = registry.getServiceExecutorWithTimeout(serviceName)) {
        WithTimeout.UNSET ->
          //use @Timeout annotation, or WithTimeout interface
          serviceMethod.withTimeout.getOrThrow()
          // else use default value
            ?: TASK_WITH_TIMEOUT_DEFAULT

        else -> wt
      }

  private fun getWithRetry(serviceName: ServiceName, serviceMethod: Method): WithRetry? =
      // use withRetry from registry, if it exists
      when (val wr = registry.getServiceExecutorWithRetry(serviceName)) {
        WithRetry.UNSET ->
          // use @Retry annotation, or WithRetry interface
          serviceMethod.withRetry.getOrThrow()
          // else use default value
            ?: TASK_WITH_RETRY_DEFAULT

        else -> wr
      }

  private suspend fun ExecuteTask.parseWorkflowData(): TaskData {
    val serviceInstance = WorkflowTaskImpl()
    val serviceMethod = WorkflowTaskImpl::handle.javaMethod!!
    val serviceArgs = serviceMethod.deserializeArgs(methodArgs)

    val workflowTaskParameters = serviceArgs[0] as WorkflowTaskParameters

    // workflow instance
    val workflowInstance = registry.getWorkflowExecutorInstance(workflowTaskParameters)

    // method of the workflow instance
    val workflowMethod = with(workflowTaskParameters) {
      workflowInstance::class.java.getMethodPerNameAndParameters(
          "${workflowMethod.methodName}",
          workflowMethod.methodParameterTypes?.types,
          workflowMethod.methodParameters.size,
      )
    }

    val workflowName = (requester as WorkflowRequester).workflowName
    // get checkMode from registry
    val checkMode = registry.getWorkflowExecutorCheckMode(workflowName)
    // else use CheckMode method annotation on method or class
      ?: workflowMethod.checkMode
      // else use default value
      ?: WORKFLOW_CHECK_MODE_DEFAULT

    serviceInstance.apply {
      this.logger = TaskExecutor.logger
      this.checkMode = checkMode
      this.instance = workflowInstance
      this.method = workflowMethod
    }

    val withTimeout = getWithTimeout(workflowName)

    val withRetry = getWithRetry(workflowName, workflowMethod)

    val context = getContext(withRetry, withTimeout)

    return TaskData(
        serviceInstance,
        serviceMethod,
        withTimeout,
        withRetry,
        false,
        serviceArgs,
        context,
    )
  }

  private fun getWithTimeout(workflowName: WorkflowName) =
      // use withTimeout from registry, if it exists
      when (val wt = registry.getWorkflowExecutorWithTimeout(workflowName)) {
        WithTimeout.UNSET ->
          // HERE WE ARE LOOKING FOR THE TIMEOUT OF THE WORKFLOW TASK
          // NOT OF THE WORKFLOW ITSELF, THAT'S WHY WE DO NOT LOOK FOR
          // THE @Timeout ANNOTATION OR THE WithTimeout INTERFACE
          // THAT HAS A DIFFERENT MEANING IN WORKFLOWS
          // else use default value
          WORKFLOW_TASK_WITH_TIMEOUT_DEFAULT

        else -> wt
      }

  private fun getWithRetry(workflowName: WorkflowName, workflowMethod: Method) =
      // use withRetry from registry, if it exists
      when (val wr = registry.getWorkflowExecutorWithRetry(workflowName)) {
        WithRetry.UNSET ->
          // else use @Retry annotation, or WithRetry interface
          workflowMethod.withRetry.getOrThrow()
          // else use default value
            ?: WORKFLOW_TASK_WITH_RETRY_DEFAULT

        else -> wr
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
    val logger = KotlinLogging.logger {}

    val TASK_WITH_TIMEOUT_DEFAULT: WithTimeout? = null
    val TASK_WITH_RETRY_DEFAULT: WithRetry? = null
    val WORKFLOW_TASK_WITH_TIMEOUT_DEFAULT = WithTimeout { 60.0 }
    val WORKFLOW_TASK_WITH_RETRY_DEFAULT: WithRetry? = null
    val WORKFLOW_CHECK_MODE_DEFAULT = WorkflowCheckMode.simple
  }
}
