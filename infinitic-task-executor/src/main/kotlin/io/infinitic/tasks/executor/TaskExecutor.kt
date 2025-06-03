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
import io.infinitic.common.data.methods.deserializeArgs
import io.infinitic.common.data.methods.encodeReturnValue
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.registry.ExecutorRegistryInterface
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.utils.BatchMethod
import io.infinitic.common.utils.checkMode
import io.infinitic.common.utils.getBatchMethod
import io.infinitic.common.utils.getInterface
import io.infinitic.common.utils.getMethodPerNameAndParameters
import io.infinitic.common.utils.isDelegated
import io.infinitic.common.utils.withRetry
import io.infinitic.common.utils.withTimeout
import io.infinitic.common.workflows.WorkflowContext
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.isWorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.DeferredException
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.UNSET_WITH_RETRY
import io.infinitic.tasks.UNSET_WITH_TIMEOUT
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.executor.task.TaskContextImpl
import io.infinitic.tasks.executor.task.TaskRunner
import io.infinitic.tasks.getMillisBeforeRetry
import io.infinitic.tasks.graceMillis
import io.infinitic.tasks.threadLocalBatchContext
import io.infinitic.tasks.threadLocalTaskContext
import io.infinitic.tasks.timeoutMillis
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.threadLocalWorkflowContext
import io.infinitic.workflows.workflowTask.WorkflowTaskImpl
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException
import kotlin.reflect.jvm.javaMethod
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


/**
 * Handles the execution of workflow and service tasks. The `TaskExecutor` coordinates
 * task execution by processing messages related to workflow or service tasks, adhering
 * to specific timeout and retry policies defined for each task type.
 *
 * This executor leverages a registry, a producer, and a client to:
 * - Retrieve configuration settings and task-specific executors
 * - Send task-related messages such as "task started", "task failed", or "task completed"
 * - Execute tasks directly or in batches, depending on task type
 *
 * The class supports detailed handling of timeouts, retries, and exceptions during task execution.
 */
class TaskExecutor(
  private val executor: ExecutorService,
  private val registry: ExecutorRegistryInterface,
  private val producer: InfiniticProducer,
  private val client: InfiniticClientInterface
) {

  /**
   * Processes a given message of type `ServiceExecutorMessage`.
   * If the message is an instance of `ExecuteTask`, it delegates the processing to the `process` method of `ExecuteTask`.
   */
  suspend fun process(msg: ServiceExecutorMessage) {
    when (msg) {
      is ExecuteTask -> msg.process()
    }
  }

  /**
   * Processes a batch of service execution messages. Depending on the type of task represented
   * by each message (workflow task or service task), the messages are processed either in parallel
   * or using a batch processing method. If a batch method is not available for service tasks,
   * the tasks are processed individually.
   *
   * @param messages a list of `ServiceExecutorMessage` objects to be processed. Each message
   * represents either a workflow task or a service task associated with a specific service and method.
   */
  suspend fun batchProcess(messages: List<ServiceExecutorMessage>) {
    val executeTasks = messages.map {
      when (it) {
        is ExecuteTask -> it
      }
    }

    // as of 0.16.0 batchProcess should have List<ServiceExecutorMessages>
    // with a unique serviceName and methodName
    // but we groupBy here, just in case this changes later
    val executeTasksMap = executeTasks.groupBy { it.serviceName to it.methodName }

    coroutineScope {
      executeTasksMap.map { (serviceNameAndMethodName, executeTasks) ->
        when (serviceNameAndMethodName.first.isWorkflowTask()) {
          // for workflow tasks, we run them in parallel
          true -> executeTasks.forEach { launch { it.process() } }
          // for services, we use the batched method
          false -> {
            val (_, serviceMethod, _) = executeTasks.first().getInstanceMethodAndReturnType()
            when (serviceMethod.getBatchMethod()) {
              // there is no batch method, we just proceed with the task one by one
              null -> executeTasks.forEach { launch { it.process() } }
              // process using the batch method.
              else -> launch { executeTasks.process() }
            }
          }
        }
      }
    }
  }


  /**
   * Retrieves the batch key associated with the given service executor message.
   * If the message is an instance of `ExecuteTask`, it delegates the retrieval to the `getBatchKey` method of `ExecuteTask`.
   */
  fun getBatchKey(msg: ServiceExecutorMessage): String? = when (msg) {
    is ExecuteTask -> msg.getBatchKey()
  }

  private fun ExecuteTask.getBatchKey(): String? = when (isWorkflowTask()) {
    true -> thisShouldNotHappen()
    false -> taskMeta[TaskMeta.BATCH_KEY]?.let { String(it) }
  }

  private data class TaskData(
    val instance: Any,
    val method: Method,
    val returnType: Type,
    val withTimeout: WithTimeout?,
    val withRetry: WithRetry?,
    val isDelegated: Boolean,
    val args: List<*>,
    val context: TaskContext
  ) {
    fun invoke(): Any? = method.invoke(instance, *args.toTypedArray())
  }

  private data class BatchData(
    val instance: Any,
    val batchMethod: BatchMethod,
    val returnType: Type,
    val withTimeout: WithTimeout?,
    val withRetry: WithRetry?,
    val isDelegated: Boolean,
    val argsMap: Map<TaskId, List<*>>,
    val contextMap: Map<TaskId, TaskContext>
  ) {
    @Suppress("UNCHECKED_CAST")
    fun invoke(): Map<String, Any?> {
      val o = batchMethod.batch.invoke(instance, batchMethod.getArgs(argsMap))
      // if null, returns Map<String, null>
        ?: argsMap.map { it.key.toString() to null }.toMap()
      return o as Map<String, Any?>
    }

    fun toMetaMap(): Map<TaskId, MutableMap<String, ByteArray>> =
        contextMap.mapValues { it.value.meta }
  }

  private suspend fun ExecuteTask.process() {
    logDebug { "Start processing $this" }

    // Signal that the task has started
    sendTaskStarted()

    // Parse the task data. If parsing fails, return without proceeding
    val taskData = parse().getOrElse { return }

    // Get the task timeout. If this operation fails, return without proceeding
    val timeoutMillis = getTimeoutMillis(taskData).getOrElse { return }

    // Get the task grace. If this operation fails, return without proceeding
    val graceMillis = getGraceMillis(taskData).getOrElse { return }

    // Execute the task with the specified timeout and grace. If this operation fails, return without proceeding
    val output = executeWithTimeout(taskData, timeoutMillis, graceMillis).getOrElse { return }

    // Signal that the task has completed successfully
    sendTaskCompleted(output, taskData)

    logTrace { "Ended processing $this" }
  }

  private suspend fun List<ExecuteTask>.process() {
    // Signal that the tasks have started
    sendTaskStarted()

    // Parse the batch. If parsing fails, return without proceeding
    val batchData = parse().getOrElse { return }

    // Get the batch timeout. If this operation fails, return without proceeding
    val timeoutMillis = getTimeoutMillis(batchData).getOrElse { return }

    // Get the task grace. If this operation fails, return without proceeding
    val graceMillis = getGraceMillis(batchData).getOrElse { return }

    // Execute the batch with the specified timeout.
    // If this operation fails, return without proceeding
    val output = executeWithTimeout(batchData, timeoutMillis, graceMillis).getOrElse { return }

    val inputTaskIds = batchData.argsMap.keys.map { it.toString() }.toSet()
    val unknownFromOutput = output.keys.subtract(inputTaskIds)
    val missingFromOutput = inputTaskIds.subtract(output.keys)

    when {
      unknownFromOutput.isNotEmpty() -> sendTaskFailed(
          Exception("Unknown keys: ${unknownFromOutput.joinToString()}}"),
          batchData.toMetaMap(),
      ) {
        "Error in the return values of the @batch ${batchData.batchMethod.batch.name} method return value"
      }

      missingFromOutput.isNotEmpty() -> sendTaskFailed(
          Exception("Missing keys: ${missingFromOutput.joinToString()}}"),
          batchData.toMetaMap(),
      ) {
        "Error in the return values of the @batch ${batchData.batchMethod.batch.name} method return value"
      }

      // All tasks have completed successfully
      else -> sendTaskCompleted(output, batchData)
    }
  }

  private suspend fun ExecuteTask.sendTaskStarted() {
    val event = TaskStartedEvent.from(this, emitterName)
    with(producer) { event.sendTo(ServiceExecutorEventTopic) }
  }


  private suspend fun List<ExecuteTask>.sendTaskStarted() {
    coroutineScope {
      forEach { executeTask ->
        launch {
          executeTask.sendTaskStarted()
        }
      }
    }
  }

  private suspend fun ExecuteTask.parse(): Result<TaskData> = try {
    Result.success(
        when (isWorkflowTask()) {
          true -> parseWorkflowTask()
          false -> parseTask()
        },
    )
  } catch (e: Exception) {
    sendTaskFailed(e, taskMeta) { "Unable to parse message $this" }
    Result.failure(e)
  }

  private suspend fun List<ExecuteTask>.parse(): Result<BatchData> = try {
    Result.success(parseBatch())
  } catch (e: Exception) {
    sendTaskFailed(e, associate { it.taskId to it.taskMeta }) {
      "Unable to parse batch of messages"
    }
    Result.failure(e)
  }

  private suspend fun getTimeoutMillis(
    withTimeout: WithTimeout?,
    onError: suspend (Throwable) -> Unit
  ): Result<Long> = withTimeout?.timeoutMillis?.fold(
      onSuccess = { Result.success(it ?: Long.MAX_VALUE) },
      onFailure = {
        onError(it)
        Result.failure(it)
      },
  ) ?: Result.success(Long.MAX_VALUE)

  private suspend fun getGraceMillis(
    withTimeout: WithTimeout?,
    onError: suspend (Throwable) -> Unit
  ): Result<Long> = withTimeout?.graceMillis?.fold(
      onSuccess = { Result.success(it) },
      onFailure = {
        onError(it)
        Result.failure(it)
      },
  ) ?: Result.success(0L)

  private suspend fun ExecuteTask.getTimeoutMillis(taskData: TaskData): Result<Long> =
      getTimeoutMillis(taskData.withTimeout) {
        sendTaskFailed(it, taskMeta) { "Error in ${WithTimeout::getTimeoutSeconds.name} method" }
      }

  private suspend fun ExecuteTask.getGraceMillis(taskData: TaskData): Result<Long> =
      getGraceMillis(taskData.withTimeout) {
        sendTaskFailed(it, taskMeta) {
          "Error in ${WithTimeout::getGracePeriodAfterTimeoutSeconds.name} method"
        }
      }

  private suspend fun List<ExecuteTask>.getTimeoutMillis(batchData: BatchData): Result<Long> =
      getTimeoutMillis(batchData.withTimeout) { e ->
        sendTaskFailed(e, associate { it.taskId to it.taskMeta }) {
          "Error in ${WithTimeout::getTimeoutSeconds.name} method"
        }
      }

  private suspend fun List<ExecuteTask>.getGraceMillis(batchData: BatchData): Result<Long> =
      getGraceMillis(batchData.withTimeout) { e ->
        sendTaskFailed(e, associate { it.taskId to it.taskMeta }) {
          "Error in ${WithTimeout::getGracePeriodAfterTimeoutSeconds.name} method"
        }
      }

  private suspend fun ExecuteTask.executeWithTimeout(
    taskData: TaskData,
    timeoutMillis: Long,
    graceMillis: Long
  ): Result<Any?> {
    val name = with(taskData.context) { "$serviceName:$taskName-$taskId" }
    // For workflow task, the workflow context is set in the thread local during the workflow instantiation
    val workflowContext: WorkflowContext? = threadLocalWorkflowContext.get()
    // Run the task with a timeout
    val result = TaskRunner(executor, logger).runWithTimeout(name, timeoutMillis, graceMillis) {
      // For the current thread, set the task context
      threadLocalTaskContext.set(taskData.context)
      // For the current thread, set the workflow context if it exists (for workflow tasks)
      workflowContext?.let { threadLocalWorkflowContext.set(it) }
      // invoke the task method
      taskData.invoke().also {
        // For the current thread, clear the workflow and task context
        threadLocalWorkflowContext.remove()
        threadLocalTaskContext.remove()
      }
    }
    // Clear the thread local context after execution
    threadLocalWorkflowContext.remove()
    // handle exceptions that may have occurred during execution
    when (val e = result.exceptionOrNull()) {
      // success
      null -> null
      // timeout exception
      is TimeoutException -> retryOrSendTaskFailed(taskData.withRetry, taskData.context, e)
      // exception within the invoke method
      is InvocationTargetException -> handleInvocationTargetException(taskData, e)
      // any other exception
      else -> sendTaskFailed(e, taskData.context.meta)
    }
    return result
  }

  private suspend fun List<ExecuteTask>.executeWithTimeout(
    batchData: BatchData,
    timeoutMillis: Long,
    graceMillis: Long
  ): Result<Map<String, Any?>> {
    val name = with(batchData) {
      "${contextMap.values.first().serviceName}:${batchMethod.batch.name}-${contextMap.keys.size}"
    }
    val result = TaskRunner(executor, logger).runWithTimeout(name, timeoutMillis, graceMillis) {
      threadLocalBatchContext.set(batchData.contextMap.mapKeys { it.key.toString() })
      batchData.invoke()
    }
    when (val e = result.exceptionOrNull()) {
      // success
      null -> null
      // timeout exception
      is TimeoutException -> retryOrSendBatchFailed(batchData.withRetry, batchData.contextMap, e)
      // exception within the invoke method
      is InvocationTargetException -> handleInvocationTargetException(batchData, e)
      // any other exception
      else -> sendTaskFailed(e, batchData.toMetaMap()) {
        "An error occurred while processing batch messages"
      }
    }
    return result
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

  private suspend fun List<ExecuteTask>.handleInvocationTargetException(
    batchData: BatchData,
    e: InvocationTargetException
  ) {
    val cause = e.cause ?: e.targetException
    when (cause) {
      is Exception -> retryOrSendBatchFailed(batchData.withRetry, batchData.contextMap, cause)
      is Throwable -> throw cause
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

  private suspend fun List<ExecuteTask>.retryOrSendBatchFailed(
    withRetry: WithRetry?,
    batchContext: Map<TaskId, TaskContext>,
    cause: Exception
  ) {
    val delaysMillis = getDelayMillis(withRetry, batchContext.values, cause)

    delaysMillis.forEachIndexed { index, result ->
      val executeTask = this[index]
      val meta = batchContext[executeTask.taskId]!!.meta

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

  private suspend fun List<ExecuteTask>.getDelayMillis(
    withRetry: WithRetry?,
    taskContexts: Collection<TaskContext>,
    cause: Exception
  ): List<Result<Long?>> =
      coroutineScope {
        mapIndexed { index, executeTask ->
          async {
            executeTask.getDelayMillis(withRetry, taskContexts.elementAt(index), cause)
          }
        }.toList().awaitAll()
      }

  suspend fun ExecuteTask.sendTaskFailed(
    cause: Throwable,
    meta: Map<String, ByteArray>,
    description: (() -> String)? = null
  ) {
    logError(cause, description)
    val event = TaskFailedEvent.from(this, emitterName, cause, meta)
    with(producer) { event.sendTo(ServiceExecutorEventTopic) }
  }

  private suspend fun List<ExecuteTask>.sendTaskFailed(
    cause: Throwable,
    meta: Map<TaskId, Map<String, ByteArray>>,
    description: (() -> String)?
  ) = coroutineScope {
    map { executeTask ->
      launch {
        executeTask.sendTaskFailed(cause, meta[executeTask.taskId]!!, description)
      }
    }
  }

  private suspend fun ExecuteTask.sendRetryTask(
    cause: Exception,
    delay: MillisDuration,
    meta: Map<String, ByteArray>
  ) {
    logWarn(cause) { "Retrying in $delay" }

    val executeTask = ExecuteTask.retryFrom(this, emitterName, delay, cause, meta)
    with(producer) { executeTask.sendTo(ServiceExecutorRetryTopic, delay) }

    // once sent, we publish the event
    val event = TaskRetriedEvent.from(this, emitterName, cause, delay, meta)
    with(producer) { event.sendTo(ServiceExecutorEventTopic) }
  }

  private suspend fun ExecuteTask.sendTaskCompleted(
    output: Any?,
    isDelegated: Boolean,
    method: Method,
    meta: Map<String, ByteArray>,
    returnType: Type
  ) {
    if (isDelegated && output != null) logDebug {
      "Method '${method}' has an '${Delegated::class.java.name}' annotation, so its result is ignored"
    }
    val returnValue = try {
      Result.success(method.encodeReturnValue(output, returnType))
    } catch (e: Exception) {
      Result.failure(e)
    }
    returnValue.onSuccess {
      val taskCompletedEvent = TaskCompletedEvent.from(
          this,
          emitterName,
          it,
          isDelegated,
          meta,
      )
      with(producer) { taskCompletedEvent.sendTo(ServiceExecutorEventTopic) }
    }

    returnValue.onFailure {
      sendTaskFailed(it, meta) {
        "Error during serialization of the task output for task $serviceName.$methodName ($taskId)"
      }
    }
  }

  private suspend fun ExecuteTask.sendTaskCompleted(output: Any?, taskData: TaskData) =
      sendTaskCompleted(
          output,
          taskData.isDelegated,
          taskData.method,
          taskData.context.meta,
          taskData.returnType,
      )

  private suspend fun List<ExecuteTask>.sendTaskCompleted(
    output: Map<String, Any?>,
    batchData: BatchData
  ) = coroutineScope {
    forEach { executeTask ->
      launch {
        executeTask.sendTaskCompleted(
            output[executeTask.taskId.toString()],
            batchData.isDelegated,
            batchData.batchMethod.single,
            batchData.contextMap[executeTask.taskId]!!.meta,
            batchData.returnType,
        )
      }
    }
  }

  private fun ExecuteTask.getInstanceMethodAndReturnType(): Triple<Any, Method, Type> {
    // Obtain the service instance from the registry
    val serviceInstance = registry.getServiceExecutorInstance(serviceName)

    // Interface defining the Service contract
    val serviceInterface = serviceInstance::class.java.getInterface(serviceName.toString())

    // Return type of the method (from the contract, not the implementation)
    val returnType = serviceInterface.getMethodPerNameAndParameters(
        "$methodName",
        methodParameterTypes?.types,
        methodArgs.size,
    ).genericReturnType

    // Method from the Service implementation
    val serviceMethod = serviceInstance::class.java.getMethodPerNameAndParameters(
        "$methodName",
        methodParameterTypes?.types,
        methodArgs.size,
    )

    // Return the class and method corresponding to the specified name and parameter types
    return Triple(serviceInstance, serviceMethod, returnType)
  }

  private fun ExecuteTask.getContext(
    withRetry: WithRetry?,
    withTimeout: WithTimeout?
  ): TaskContext = TaskContextImpl(
      workerName = emitterName.toString(),
      serviceName = serviceName,
      taskId = taskId,
      taskName = methodName,
      workflowId = requester.workflowId,
      workflowName = requester.workflowName,
      workflowVersion = requester.workflowVersion,
      retrySequence = taskRetrySequence,
      retryIndex = taskRetryIndex,
      lastError = lastFailure,
      tags = taskTags.map { it.tag }.toSet(),
      meta = taskMeta.map.toMutableMap(),
      withRetry = withRetry,
      withTimeout = withTimeout,
      client = client,
  )

  private fun ExecuteTask.parseBatch(): TaskData {
    val (serviceInstance, serviceMethod, returnType) = getInstanceMethodAndReturnType()

    val batchMethod = serviceMethod.getBatchMethod() ?: thisShouldNotHappen()

    val withTimeout = getWithTimeout(serviceName, batchMethod.batch)

    val withRetry = getWithRetry(serviceName, batchMethod.batch)

    val isDelegated = batchMethod.single.isDelegated || batchMethod.batch.isDelegated

    val taskContext = getContext(withRetry, withTimeout)

    return TaskData(
        serviceInstance,
        serviceMethod,
        returnType,
        withTimeout,
        withRetry,
        isDelegated,
        listOf<Unit>(),
        taskContext,
    )
  }

  private suspend fun List<ExecuteTask>.parseBatch(): BatchData {
    // Select the first task or throw an exception if the list is empty
    val executeTask = firstOrNull() ?: thisShouldNotHappen()

    // Parse the task data
    val taskData = executeTask.parseBatch()

    // Deserialize the method arguments for each task, asynchronously as it can be expensive
    val argsList = coroutineScope {
      map { async { taskData.method.deserializeArgs(it.methodArgs) } }.toList().awaitAll()
    }

    // list of task IDs
    val taskIds = map { it.taskId }

    // map of args by task ID
    val argsMap = taskIds.zip(argsList).toMap()

    // Retrieve the context for each task
    val contextMap = associate {
      it.taskId to it.getContext(taskData.withRetry, taskData.withTimeout)
    }

    // Return BatchTaskData containing the task information and contexts
    return BatchData(
        instance = taskData.instance,
        batchMethod = taskData.method.getBatchMethod() ?: thisShouldNotHappen(),
        returnType = taskData.returnType,
        withTimeout = taskData.withTimeout,
        withRetry = taskData.withRetry,
        isDelegated = taskData.isDelegated,
        argsMap = argsMap,
        contextMap = contextMap,
    )
  }

  private fun ExecuteTask.parseTask(): TaskData {

    val (serviceInstance, serviceMethod, returnType) = getInstanceMethodAndReturnType()

    val serviceArgs = serviceMethod.deserializeArgs(methodArgs)

    val withTimeout = getWithTimeout(serviceName, serviceMethod)

    val withRetry = getWithRetry(serviceName, serviceMethod)

    // check if this method has the @Delegate annotation
    val isDelegated = serviceMethod.isDelegated

    val taskContext = getContext(withRetry, withTimeout)

    return TaskData(
        serviceInstance,
        serviceMethod,
        returnType,
        withTimeout,
        withRetry,
        isDelegated,
        serviceArgs,
        taskContext,
    )
  }

  private fun ExecuteTask.parseWorkflowTask(): TaskData {
    val serviceInstance = WorkflowTaskImpl()
    val serviceMethod = WorkflowTaskImpl::handle.javaMethod!!
    val serviceArgs = serviceMethod.deserializeArgs(methodArgs)

    val workflowTaskParameters = serviceArgs[0] as WorkflowTaskParameters
    val methodName = workflowTaskParameters.workflowMethod.methodName.toString()
    val methodParametersType = workflowTaskParameters.workflowMethod.methodParameterTypes?.types
    val methodParametersSize = workflowTaskParameters.workflowMethod.methodParameters.size

    // workflow instance
    val workflowInstance = registry.getWorkflowExecutorInstance(workflowTaskParameters)

    // method of the workflow instance
    val workflowMethod = workflowInstance::class.java.getMethodPerNameAndParameters(
        methodName,
        methodParametersType,
        methodParametersSize,
    )

    // Interface defining the workflow contract
    val workflowInterface =
        workflowInstance::class.java.getInterface(workflowTaskParameters.workflowName.toString())

    // Return type of the method (from the contract, not the implementation)
    val returnType = workflowInterface.getMethodPerNameAndParameters(
        methodName,
        methodParametersType,
        methodParametersSize,
    ).genericReturnType

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
      this.returnType = returnType
    }

    val withTimeout = getWithTimeout(workflowName)

    val withRetry = getWithRetry(workflowName, workflowMethod)

    val context = getContext(withRetry, withTimeout)

    return TaskData(
        serviceInstance,
        serviceMethod,
        serviceMethod.genericReturnType,
        withTimeout,
        withRetry,
        false,
        serviceArgs,
        context,
    )
  }

  /**
   * Retrieves the `WithTimeout` setting for a given service and method.
   */
  private fun getWithTimeout(serviceName: ServiceName, serviceMethod: Method): WithTimeout? =
      // configuration set in Service registration supersedes configuration from sources
      when (val wt = registry.getServiceExecutorWithTimeout(serviceName)) {
        UNSET_WITH_TIMEOUT ->
          //use @Timeout annotation, or WithTimeout interface
          serviceMethod.withTimeout.getOrThrow()
          // else use default value
            ?: TASK_WITH_TIMEOUT_DEFAULT

        else -> wt
      }

  /**
   * Retrieves the `WithTimeout` setting for a given workflow task.
   */
  private fun getWithTimeout(workflowName: WorkflowName) =
      // configuration set in Workflow registration supersedes configuration from sources
      when (val wt = registry.getWorkflowExecutorWithTimeout(workflowName)) {
        UNSET_WITH_TIMEOUT ->
          // HERE WE ARE LOOKING FOR THE TIMEOUT OF THE WORKFLOW TASK
          // NOT OF THE WORKFLOW ITSELF, THAT'S WHY WE DO NOT LOOK FOR
          // THE @Timeout ANNOTATION OR THE WithTimeout INTERFACE
          // THAT HAS A DIFFERENT MEANING IN WORKFLOWS
          // else use default value
          WORKFLOW_TASK_WITH_TIMEOUT_DEFAULT

        else -> wt
      }

  /**
   * Retrieves the `WithRetry` setting for a given service and method.
   */
  private fun getWithRetry(serviceName: ServiceName, serviceMethod: Method): WithRetry? =
      // use withRetry from the registry if it exists
      when (val wr = registry.getServiceExecutorWithRetry(serviceName)) {
        UNSET_WITH_RETRY ->
          // use @Retry annotation, or WithRetry interface
          serviceMethod.withRetry.getOrThrow()
          // else use default value
            ?: TASK_WITH_RETRY_DEFAULT

        else -> wr
      }

  /**
   * Retrieves the `WithRetry` setting for a given workflow task.
   */
  private fun getWithRetry(workflowName: WorkflowName, workflowMethod: Method) =
      // use withRetry from the registry if it exists
      when (val wr = registry.getWorkflowExecutorWithRetry(workflowName)) {
        UNSET_WITH_RETRY ->
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
    val logger = LoggerWithCounter(KotlinLogging.logger {})

    val TASK_WITH_TIMEOUT_DEFAULT: WithTimeout? = null
    val TASK_WITH_RETRY_DEFAULT: WithRetry? = null
    val WORKFLOW_TASK_WITH_TIMEOUT_DEFAULT = WithTimeout { 60.0 }
    val WORKFLOW_TASK_WITH_RETRY_DEFAULT: WithRetry? = null
    val WORKFLOW_CHECK_MODE_DEFAULT = WorkflowCheckMode.simple
  }
}
