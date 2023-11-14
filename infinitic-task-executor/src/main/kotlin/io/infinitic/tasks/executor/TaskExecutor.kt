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

import io.infinitic.annotations.CheckMode
import io.infinitic.annotations.Retry
import io.infinitic.annotations.Timeout
import io.infinitic.annotations.getInstance
import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.tasks.executors.errors.FailedTaskError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.exceptions.DeferredException
import io.infinitic.exceptions.tasks.TimeoutException
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.task.TaskContextImpl
import io.infinitic.tasks.getTimeoutInMillis
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.workflowTask.WorkflowTaskImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedWorkflow
import io.infinitic.common.workflows.engine.messages.TaskFailed as TaskFailedWorkflow

class TaskExecutor(
  private val workerRegistry: WorkerRegistry,
  private val producer: InfiniticProducer,
  private val client: InfiniticClientInterface
) {
  companion object {
    val DEFAULT_WORKFLOW_TASK_TIMEOUT = WithTimeout { 60.0 }
    val DEFAULT_TASK_TIMEOUT = null
    val DEFAULT_TASK_RETRY = RetryPolicy.DEFAULT
    val DEFAULT_WORKFLOW_TASK_RETRY = null
    val DEFAULT_WORKFLOW_CHECK_MODE = WorkflowCheckMode.simple
  }

  private val logger = KotlinLogging.logger {}

  private var withRetry: WithRetry? = null

  private var withTimeout: WithTimeout? = null

  private val clientName = ClientName(producer.name)

  suspend fun handle(message: TaskExecutorMessage) {
    logger.debug { "receiving $message" }

    return when (message) {
      is ExecuteTask -> executeTask(message)
    }
  }

  private suspend fun executeTask(message: ExecuteTask) = coroutineScope {
    // trying to instantiate the task
    val (service, method, parameters) =
        try {
          parse(message)
        } catch (e: Exception) {
          // returning the exception (no retry)
          sendTaskFailed(message, e)
          // stop here
          return@coroutineScope
        }

    val taskContext =
        TaskContextImpl(
            workerName = producer.name,
            workerRegistry = workerRegistry,
            serviceName = message.serviceName,
            taskId = message.taskId,
            taskName = message.methodName,
            workflowId = message.workflowId,
            workflowName = message.workflowName,
            workflowVersion = message.workflowVersion,
            retrySequence = message.taskRetrySequence,
            retryIndex = message.taskRetryIndex,
            lastError = message.lastError,
            tags = message.taskTags.map { it.tag }.toSet(),
            meta = message.taskMeta.map.toMutableMap(),
            withRetry = withRetry,
            withTimeout = withTimeout,
            client = client,
        )

    try {
      val millis = withTimeout?.getTimeoutInMillis()
      val output =
          if (millis != null && millis > 0) {
            withTimeout(millis) { runTask(service, method, parameters, taskContext) }
          } else {
            runTask(service, method, parameters, taskContext)
          }
      sendTaskCompleted(message, output, taskContext.meta)
    } catch (e: InvocationTargetException) {
      // exception in method execution
      when (val cause = e.cause) {
        // do not retry failed workflow task due to failed/canceled task/workflow
        is DeferredException -> sendTaskFailed(message, cause)
        // simple exception
        is Exception -> failTaskWithRetry(taskContext, cause, message)
        // Throwable are not caught
        else -> throw cause ?: e
      }
    } catch (e: TimeoutCancellationException) {
      val cause = TimeoutException(service.javaClass.name, withTimeout!!.getTimeoutInSeconds()!!)
      // returning a timeout
      failTaskWithRetry(taskContext, cause, message)
    } catch (e: Exception) {
      // returning the exception (no retry)
      sendTaskFailed(message, e)
    }
  }

  private suspend fun runTask(
    service: Any,
    method: Method,
    parameters: Array<Any?>,
    taskContext: TaskContext
  ): Any? = coroutineScope {
    // context is stored in execution's thread (in case used in method)
    Task.context.set(taskContext)
    // execution
    method.invoke(service, *parameters)
  }

  private suspend fun failTaskWithRetry(
    taskContext: TaskContext,
    cause: Exception,
    msg: ExecuteTask
  ) {
    val delay =
        try {
          // context is stored in execution's thread (in case used in retryable)
          Task.context.set(taskContext)
          // get seconds before retry
          withRetry?.getSecondsBeforeRetry(taskContext.retryIndex.toInt(), cause)
        } catch (e: Exception) {
          logger.error(e) {
            "Error in ${WithRetry::class.java.simpleName} ${withRetry!!::class.java.name}"
          }
          // no retry
          sendTaskFailed(msg, e)

          return
        }

    when (delay) {
      null -> sendTaskFailed(msg, cause)
      else -> retryTask(msg, cause, MillisDuration((delay * 1000).toLong()), taskContext.meta)
    }
  }

  private fun parse(msg: ExecuteTask): TaskCommand {
    val service =
        when (msg.isWorkflowTask()) {
          true -> WorkflowTaskImpl()
          false -> workerRegistry.getRegisteredService(msg.serviceName).factory()
        }

    val taskMethod =
        getMethodPerNameAndParameters(
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
        val workflowMethod =
            with(workflowTaskParameters) {
              // method instance
              getMethodPerNameAndParameters(
                  workflow::class.java,
                  "${methodRun.methodName}",
                  methodRun.methodParameterTypes?.types,
                  methodRun.methodParameters.size,
              )
            }
        this.withTimeout =
            // use timeout from registry, if it exists
            // else use withTimeout from method annotation
            registered.withTimeout
              ?: workflowMethod.getAnnotation(Timeout::class.java)?.getInstance()
                  // else use withRetry from class annotation
                  ?: workflow::class.java.getAnnotation(Timeout::class.java)?.getInstance()
                  // else use workflow if WithTimeout
                  ?: when (workflow) {
                is WithTimeout -> workflow
                else -> null
              }
                  // else use default value
                  ?: DEFAULT_WORKFLOW_TASK_TIMEOUT

        this.withRetry = // use timeout from registry, if it exists
            // else use withTimeout from method annotation
            registered.withRetry
              ?: workflowMethod.getAnnotation(Retry::class.java)?.getInstance()
                  // else use withRetry from class annotation
                  ?: workflow::class.java.getAnnotation(Retry::class.java)?.getInstance()
                  // else use workflow if WithTimeout
                  ?: when (workflow) {
                is WithRetry -> workflow
                else -> null
              }
                  // else use default value
                  ?: DEFAULT_WORKFLOW_TASK_RETRY

        val checkMode =
        // get checkMode from registry
            // else get mode from method annotation
            registered.checkMode
              ?: workflowMethod.getAnnotation(CheckMode::class.java)?.mode
              // else get mode from class annotation
              ?: workflow::class.java.getAnnotation(CheckMode::class.java)?.mode
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
            // else use withTimeout from method annotation
            registered.withTimeout
              ?: taskMethod.getAnnotation(Timeout::class.java)?.getInstance()
                  // else use withTimeout from class annotation
                  ?: service::class.java.getAnnotation(Timeout::class.java)?.getInstance()
                  // else use service if WithRetry
                  ?: when (service) {
                is WithTimeout -> service
                else -> null
              }
                  // else use default value
                  ?: DEFAULT_TASK_TIMEOUT

        this.withRetry =
            // use withRetry from registry, if it exists
            // else use withRetry from method annotation
            registered.withRetry
              ?: taskMethod.getAnnotation(Retry::class.java)?.getInstance()
                  // else use withRetry from class annotation
                  ?: service::class.java.getAnnotation(Retry::class.java)?.getInstance()
                  // else use service if WithRetry
                  ?: when (service) {
                is WithRetry -> service
                else -> null
              }
                  // else use default value
                  ?: DEFAULT_TASK_RETRY
      }
    }

    return TaskCommand(service, taskMethod, parameters)
  }

  private fun retryTask(
    message: ExecuteTask,
    exception: Exception,
    delay: MillisDuration,
    meta: Map<String, ByteArray>
  ) {
    logger.info(exception) {
      "Retrying task '${message.serviceName}' (${message.taskId}) after $delay ms"
    }

    val executeTask =
        message.copy(
            taskRetryIndex = message.taskRetryIndex + 1,
            lastError = getExecutionError(exception),
            taskMeta = TaskMeta(meta),
        )

    producer.sendAsync(executeTask, delay).join()
  }

  private suspend fun sendTaskFailed(message: ExecuteTask, throwable: Throwable) = coroutineScope {
    logger.info(throwable) { "Task failed '${message.serviceName}' (${message.taskId})" }

    val workerError = getExecutionError(throwable)

    if (message.clientWaiting) {
      val taskFailed =
          TaskFailedClient(
              recipientName = message.emitterName,
              taskId = message.taskId,
              cause = workerError,
              emitterName = clientName,
          )
      launch { producer.sendAsync(taskFailed) }
    }

    if (message.workflowId != null) {
      val taskFailed =
          TaskFailedWorkflow(
              workflowName = message.workflowName ?: thisShouldNotHappen(),
              workflowId = message.workflowId ?: thisShouldNotHappen(),
              methodRunId = message.methodRunId ?: thisShouldNotHappen(),
              failedTaskError =
              FailedTaskError(
                  serviceName = message.serviceName,
                  taskId = message.taskId,
                  methodName = message.methodName,
                  cause = workerError,
              ),
              deferredError = getDeferredError(throwable),
              emitterName = clientName,
          )
      launch { producer.send(taskFailed) }
    }

    launch { removeTags(message) }
  }

  private suspend fun sendTaskCompleted(
    message: ExecuteTask,
    value: Any?,
    meta: Map<String, ByteArray>
  ) = coroutineScope {
    logger.debug { "Task completed '${message.serviceName}' (${message.taskId})" }

    val taskMeta = TaskMeta(meta)

    val returnValue = ReturnValue.from(value)

    if (message.clientWaiting) {
      val taskCompleted =
          TaskCompletedClient(
              recipientName = message.emitterName,
              taskId = message.taskId,
              taskReturnValue = returnValue,
              taskMeta = taskMeta,
              emitterName = clientName,
          )

      launch { producer.send(taskCompleted) }
    }

    if (message.workflowId != null) {
      val taskCompleted =
          TaskCompletedWorkflow(
              workflowName = message.workflowName ?: thisShouldNotHappen(),
              workflowId = message.workflowId ?: thisShouldNotHappen(),
              methodRunId = message.methodRunId ?: thisShouldNotHappen(),
              taskReturnValue =
              TaskReturnValue(
                  serviceName = message.serviceName,
                  taskId = message.taskId,
                  taskMeta = taskMeta,
                  returnValue = returnValue,
              ),
              emitterName = clientName,
          )

      launch { producer.send(taskCompleted) }
    }

    launch { removeTags(message) }
  }

  private suspend fun removeTags(message: ExecuteTask) = coroutineScope {
    message.taskTags.map {
      val removeTagFromTask =
          RemoveTagFromTask(
              taskTag = it,
              serviceName = message.serviceName,
              taskId = message.taskId,
              emitterName = clientName,
          )
      launch { producer.send(removeTagFromTask) }
    }
  }

  private fun getDeferredError(throwable: Throwable) =
      when (throwable is DeferredException) {
        true -> DeferredError.from(throwable)
        false -> null
      }

  private fun getExecutionError(throwable: Throwable) = ExecutionError.from(clientName, throwable)
}
