/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tasks.executor

import io.infinitic.annotations.Timeout
import io.infinitic.annotations.getInstance
import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.SendToTaskExecutorAfter
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.tasks.executors.errors.FailedTaskError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.exceptions.DeferredException
import io.infinitic.exceptions.tasks.TimeoutTaskException
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.task.TaskContextImpl
import io.infinitic.tasks.getTimeoutInMillis
import io.infinitic.workflows.workflowTask.WorkflowTaskImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import io.infinitic.annotations.Retry as RetryableAnnotation
import io.infinitic.common.clients.messages.TaskCompleted as TaskCompletedClient
import io.infinitic.common.clients.messages.TaskFailed as TaskFailedClient
import io.infinitic.common.workflows.engine.messages.TaskCompleted as TaskCompletedWorkflow
import io.infinitic.common.workflows.engine.messages.TaskFailed as TaskFailedWorkflow

class TaskExecutor(
    private val clientName: ClientName,
    private val workerRegistry: WorkerRegistry,
    private val sendToTaskExecutorAfter: SendToTaskExecutorAfter,
    private val sendToTaskTag: SendToTaskTag,
    private val sendToWorkflowEngine: SendToWorkflowEngine,
    private val sendToClient: SendToClient,
    private val clientFactory: ClientFactory
) {

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: TaskExecutorMessage) {
        logger.debug { "receiving $message" }

        return when (message) {
            is ExecuteTask -> executeTask(message)
        }
    }

    private suspend fun executeTask(message: ExecuteTask) = coroutineScope {
        // trying to instantiate the task
        val (service, method, parameters, withTimeout, retryable) = try {
            parse(message)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskFailed(message, e)
            // stop here
            return@coroutineScope
        }

        val taskContext = TaskContextImpl(
            workerName = "$clientName",
            workerRegistry = workerRegistry,
            serviceName = message.serviceName.toString(),
            taskId = message.taskId.toString(),
            taskName = message.methodName.toString(),
            workflowId = message.workflowId?.toString(),
            workflowName = message.workflowName?.name,
            retrySequence = message.taskRetrySequence.int,
            retryIndex = message.taskRetryIndex.int,
            lastError = message.lastError,
            tags = message.taskTags.map { it.tag }.toSet(),
            meta = message.taskMeta.map.toMutableMap(),
            withTimeout = withTimeout,
            withRetry = retryable,
            clientFactory = clientFactory
        )

        try {
            val millis = withTimeout?.getTimeoutInMillis()
            val output = if (millis != null && millis > 0) {
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
            val cause = TimeoutTaskException(service.javaClass.name, withTimeout!!.getTimeoutInSeconds()!!)
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
        val delay = try {
            // context is stored in execution's thread (in case used in retryable)
            Task.context.set(taskContext)
            // get seconds before retry
            taskContext.withRetry?.getSecondsBeforeRetry(taskContext.retryIndex, cause)
        } catch (e: Exception) {
            logger.error(e) { "Error in ${WithRetry::class.java.simpleName} ${taskContext.withRetry!!::class.java.name}" }
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
        val service = when (msg.isWorkflowTask()) {
            true -> WorkflowTaskImpl(workerRegistry.getRegisteredWorkflow(msg.workflowName!!).checkMode)
            false -> workerRegistry.getRegisteredService(msg.serviceName).factory()
        }

        val method = getMethodPerNameAndParameters(
            service::class.java,
            "${msg.methodName}",
            msg.methodParameterTypes?.types,
            msg.methodParameters.size
        )

        val parameters = msg.methodParameters.map { it.deserialize() }.toTypedArray()

        val withTimeout =
            // use timeout from registry, if it exists
            when (msg.isWorkflowTask()) {
                true -> workerRegistry.getRegisteredWorkflow(msg.workflowName!!).withTimeout
                false -> workerRegistry.getRegisteredService(msg.serviceName).withTimeout
            } // else use timeout from method
                ?: method.getAnnotation(Timeout::class.java)?.getInstance()
                // else use timeout from class
                ?: service::class.java.getAnnotation(Timeout::class.java)?.getInstance()

        val withRetry =
            // use retryable from registry, if it exists
            when (msg.isWorkflowTask()) {
                true -> workerRegistry.getRegisteredWorkflow(msg.workflowName!!).withRetry
                false -> workerRegistry.getRegisteredService(msg.serviceName).withRetry
            } // else use retryable from method
                ?: method.getAnnotation(RetryableAnnotation::class.java)?.getInstance()
                // else use retryable from class
                ?: service::class.java.getAnnotation(RetryableAnnotation::class.java)?.getInstance()
                // else use service if Retryable
                ?: when (service) {
                    is WithRetry -> service
                    else -> null
                }

        return TaskCommand(service, method, parameters, withTimeout, withRetry)
    }

    private fun retryTask(
        message: ExecuteTask,
        exception: Exception,
        delay: MillisDuration,
        meta: Map<String, ByteArray>
    ) {
        logger.info(exception) { "Retrying task '${message.serviceName}' (${message.taskId}) after $delay ms" }

        val executeTask = message.copy(
            taskRetryIndex = message.taskRetryIndex + 1,
            lastError = getExecutionError(exception),
            taskMeta = TaskMeta(meta)
        )

        sendToTaskExecutorAfter(executeTask, delay)
    }

    private suspend fun sendTaskFailed(
        message: ExecuteTask,
        throwable: Throwable
    ) = coroutineScope {
        logger.info(throwable) { "Task failed '${message.serviceName}' (${message.taskId})" }

        val workerError = getExecutionError(throwable)

        if (message.clientWaiting) {
            val taskFailed = TaskFailedClient(
                recipientName = message.emitterName,
                taskId = message.taskId,
                cause = workerError,
                emitterName = clientName
            )
            launch { sendToClient(taskFailed) }
        }

        if (message.workflowId != null) {
            val taskFailed = TaskFailedWorkflow(
                workflowName = message.workflowName ?: thisShouldNotHappen(),
                workflowId = message.workflowId ?: thisShouldNotHappen(),
                methodRunId = message.methodRunId ?: thisShouldNotHappen(),
                failedTaskError = FailedTaskError(
                    serviceName = message.serviceName,
                    taskId = message.taskId,
                    methodName = message.methodName,
                    cause = workerError
                ),
                deferredError = getDeferredError(throwable),
                emitterName = clientName
            )
            launch { sendToWorkflowEngine(taskFailed) }
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
            val taskCompleted = TaskCompletedClient(
                recipientName = message.emitterName,
                taskId = message.taskId,
                taskReturnValue = returnValue,
                taskMeta = taskMeta,
                emitterName = clientName
            )

            launch { sendToClient(taskCompleted) }
        }

        if (message.workflowId != null) {
            val taskCompleted = TaskCompletedWorkflow(
                workflowName = message.workflowName ?: thisShouldNotHappen(),
                workflowId = message.workflowId ?: thisShouldNotHappen(),
                methodRunId = message.methodRunId ?: thisShouldNotHappen(),
                taskReturnValue = TaskReturnValue(
                    serviceName = message.serviceName,
                    taskId = message.taskId,
                    taskMeta = taskMeta,
                    returnValue = returnValue
                ),
                emitterName = clientName
            )

            launch { sendToWorkflowEngine(taskCompleted) }
        }

        launch { removeTags(message) }
    }

    private suspend fun removeTags(message: ExecuteTask) = coroutineScope {
        message.taskTags.map {
            val removeTagFromTask = RemoveTagFromTask(
                taskTag = it,
                serviceName = message.serviceName,
                taskId = message.taskId,
                emitterName = clientName
            )
            launch { sendToTaskTag(removeTagFromTask) }
        }
    }

    private fun getDeferredError(throwable: Throwable) = when (throwable is DeferredException) {
        true -> DeferredError.from(throwable)
        false -> null
    }

    private fun getExecutionError(throwable: Throwable) = ExecutionError.from(clientName, throwable)
}
