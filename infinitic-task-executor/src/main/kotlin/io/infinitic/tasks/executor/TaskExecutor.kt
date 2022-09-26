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

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.clients.SendToClient
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.executors.SendToTaskExecutorAfter
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.tasks.executors.errors.FailedTaskError
import io.infinitic.common.tasks.executors.errors.WorkerError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.exceptions.DeferredException
import io.infinitic.exceptions.tasks.MaxRunDurationException
import io.infinitic.tasks.Task
import io.infinitic.tasks.executor.task.DurationBeforeRetryFailed
import io.infinitic.tasks.executor.task.DurationBeforeRetryRetrieved
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.task.TaskContextImpl
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
        val taskContext = TaskContextImpl(
            workerName = "$clientName",
            workerRegistry = workerRegistry,
            id = message.taskId.toString(),
            name = message.taskName.toString(),
            workflowId = message.workflowId?.toString(),
            workflowName = message.workflowName?.name,
            retrySequence = message.taskRetrySequence.int,
            retryIndex = message.taskRetryIndex.int,
            lastError = message.lastError,
            tags = message.taskTags.map { it.tag }.toSet(),
            meta = message.taskMeta.map.toMutableMap(),
            options = message.taskOptions,
            clientFactory = clientFactory
        )

        // trying to instantiate the task
        val (task, method, parameters, options) = try {
            parse(message)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskFailed(message, e)
            // stop here
            return@coroutineScope
        }

        // set taskContext into task
        task.context = taskContext

        try {
            val output = if (options.maxRunDuration != null && options.maxRunDuration!!.toMillis() > 0) {
                withTimeout(options.maxRunDuration!!.toMillis()) {
                    runTask(method, task, parameters)
                }
            } else {
                runTask(method, task, parameters)
            }
            sendTaskCompleted(message, output, getMeta(task))
        } catch (e: InvocationTargetException) {
            when (val cause = e.cause) {
                // do not retry failed workflow task due to failed/canceled task/workflow
                is DeferredException -> sendTaskFailed(message, cause)
                // simple exception
                is Exception -> failTaskWithRetry(task, message, cause)
                // Throwable are not caught
                else -> throw cause!!
            }
        } catch (e: TimeoutCancellationException) {
            val cause = MaxRunDurationException(task.javaClass.name, options.maxRunDuration!!)
            // returning a timeout
            failTaskWithRetry(task, message, cause)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskFailed(message, e)
        }
    }

    private fun getMeta(task: Task) = TaskMeta(task.context.meta)

    private suspend fun runTask(method: Method, task: Any, methodParameters: MethodParameters): Any? = coroutineScope {
        val parameter = methodParameters.map { it.deserialize() }.toTypedArray()
        method.invoke(task, *parameter)
    }

    private suspend fun failTaskWithRetry(
        task: Task,
        msg: ExecuteTask,
        cause: Exception
    ) {
        when (val delay = getDurationBeforeRetry(task, cause)) {
            is DurationBeforeRetryRetrieved -> {
                when (delay.value) {
                    null -> sendTaskFailed(msg, cause)
                    else -> retryTask(msg, cause, MillisDuration(delay.value.toMillis()), getMeta(task))
                }
            }

            is DurationBeforeRetryFailed -> {
                // no retry
                sendTaskFailed(msg, delay.error)
            }
        }
    }

    private fun parse(msg: ExecuteTask): TaskCommand {
        val task = when (msg.taskName) {
            TaskName(WorkflowTask::class.java.name) -> WorkflowTaskImpl()
            else -> workerRegistry.getTaskInstance(msg.taskName)
        }

        val parameterTypes = msg.methodParameterTypes

        val method = getMethodPerNameAndParameters(
            task::class.java,
            "${msg.methodName}",
            parameterTypes?.types,
            msg.methodParameters.size
        )

        return TaskCommand(task, method, msg.methodParameters, msg.taskOptions)
    }

    private fun getDurationBeforeRetry(task: Task, cause: Exception) = try {
        DurationBeforeRetryRetrieved(task.getDurationBeforeRetry(cause))
    } catch (e: Exception) {
        logger.info(cause) { "Exception in class '${task::class.java.name}' (${task.context.id})" }
        logger.info(e) { "error when executing getDurationBeforeRetry method" }
        DurationBeforeRetryFailed(e)
    }

    private fun retryTask(
        message: ExecuteTask,
        exception: Exception,
        delay: MillisDuration,
        taskMeta: TaskMeta
    ) {
        logger.info(exception) { "Retrying task '${message.taskName}' (${message.taskId}) after $delay ms" }

        val executeTask = message.copy(
            taskRetryIndex = message.taskRetryIndex + 1,
            lastError = getWorkerError(exception),
            taskMeta = taskMeta
        )

        sendToTaskExecutorAfter(executeTask, delay)
    }

    private suspend fun sendTaskFailed(
        message: ExecuteTask,
        throwable: Throwable
    ) = coroutineScope {
        logger.info(throwable) { "Task failed '${message.taskName}' (${message.taskId})" }

        val workerError = getWorkerError(throwable)

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
                    taskName = message.taskName,
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
        taskMeta: TaskMeta
    ) = coroutineScope {
        logger.debug { "Task completed '${message.taskName}' (${message.taskId})" }

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
                    taskName = message.taskName,
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
                taskName = message.taskName,
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

    private fun getWorkerError(throwable: Throwable) = WorkerError.from(clientName, throwable)
}
