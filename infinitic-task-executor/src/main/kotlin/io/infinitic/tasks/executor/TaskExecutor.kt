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

import io.infinitic.client.InfiniticClient
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.errors.Error
import io.infinitic.common.parser.getMethodPerNameAndParameters
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.exceptions.tasks.ProcessingTimeoutException
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.executor.task.DurationBeforeRetryFailed
import io.infinitic.tasks.executor.task.DurationBeforeRetryRetrieved
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.task.TaskContextImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class TaskExecutor(
    private val clientName: ClientName,
    private val taskExecutorRegister: TaskExecutorRegister,
    private val sendToTaskEngine: SendToTaskEngine,
    private val clientFactory: () -> InfiniticClient
) : TaskExecutorRegister by taskExecutorRegister {

    private val logger = KotlinLogging.logger {}

    suspend fun handle(message: TaskExecutorMessage) {
        logger.debug { "receiving $message" }

        when (message) {
            is ExecuteTaskAttempt -> executeTaskAttempt(message)
        }
    }

    private suspend fun executeTaskAttempt(message: ExecuteTaskAttempt) {
        val taskContext = TaskContextImpl(
            register = this,
            id = message.taskId.toString(),
            workflowId = message.workflowId?.toString(),
            workflowName = message.workflowName?.name,
            attemptId = message.taskAttemptId.toString(),
            retrySequence = message.taskRetrySequence.int,
            retryIndex = message.taskRetryIndex.int,
            lastError = message.lastError,
            tags = message.taskTags.map { it.tag }.toSet(),
            meta = message.taskMeta.map.toMutableMap(),
            options = message.taskOptions,
            clientFactory
        )

        // trying to instantiate the task
        val (task, method, parameters, options) = try {
            parse(message)
        } catch (e: Throwable) {
            logger.error(e) {}
            // returning the exception (no retry)
            sendTaskAttemptFailed(message, e, null, message.taskMeta)
            // stop here
            return
        }

        // set taskContext into task
        task.context = taskContext

        try {
            val output = if (options.runningTimeout != null && options.runningTimeout!! > 0F) {
                withTimeout((1000 * options.runningTimeout!!).toLong()) {
                    runTask(method, task, parameters)
                }
            } else {
                runTask(method, task, parameters)
            }
            sendTaskCompleted(message, output, TaskMeta(task.context.meta))
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            if (cause is Exception) {
                failTaskWithRetry(task, message, cause)
            } else {
                sendTaskAttemptFailed(message, cause ?: e, null, TaskMeta(task.context.meta))
            }
        } catch (e: TimeoutCancellationException) {
            val cause = ProcessingTimeoutException(task.javaClass.name, options.runningTimeout!!)
            // returning a timeout
            failTaskWithRetry(task, message, cause)
        } catch (e: Throwable) {
            // returning the exception (no retry)
            sendTaskAttemptFailed(message, e, null, TaskMeta(task.context.meta))
        }
    }

    private suspend fun runTask(method: Method, task: Any, methodParameters: MethodParameters) = coroutineScope {
        val parameter = methodParameters.map { it.deserialize() }.toTypedArray()
        val output = method.invoke(task, *parameter)
        ensureActive()
        output
    }

    private fun failTaskWithRetry(
        task: Task,
        msg: ExecuteTaskAttempt,
        cause: Exception
    ) {
        when (val delay = getDurationBeforeRetry(task, cause)) {
            is DurationBeforeRetryRetrieved -> {
                // returning the original cause
                sendTaskAttemptFailed(
                    msg,
                    cause,
                    delay.value?.let { MillisDuration(it.toMillis()) },
                    TaskMeta(task.context.meta)
                )
            }
            is DurationBeforeRetryFailed -> {
                // no retry
                sendTaskAttemptFailed(
                    msg,
                    delay.error,
                    null,
                    TaskMeta(task.context.meta)
                )
            }
        }
    }

    private fun parse(msg: ExecuteTaskAttempt): TaskCommand {
        val task = getTaskInstance("${msg.taskName}")

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
    } catch (e: Throwable) {
        logger.error(e) {
            "task ${task::class.java.name}: (${task.context.id})" +
                "error when executing getDurationBeforeRetry method with $cause: " +
                "$e"
        }
        DurationBeforeRetryFailed(e)
    }

    private fun sendTaskAttemptFailed(
        message: ExecuteTaskAttempt,
        cause: Throwable,
        delay: MillisDuration?,
        taskMeta: TaskMeta
    ) {
        logger.error(cause) { "task ${message.taskName} (${message.taskId}) - error: $cause" }

        val taskAttemptFailed = TaskAttemptFailed(
            taskName = message.taskName,
            taskId = message.taskId,
            taskAttemptDelayBeforeRetry = delay,
            taskAttemptId = message.taskAttemptId,
            taskRetrySequence = message.taskRetrySequence,
            taskRetryIndex = message.taskRetryIndex,
            taskAttemptError = Error.from(cause),
            taskMeta = taskMeta,
            emitterName = clientName
        )

        sendToTaskEngine(taskAttemptFailed)
    }

    private fun sendTaskCompleted(
        message: ExecuteTaskAttempt,
        returnValue: Any?,
        taskMeta: TaskMeta
    ) {
        val taskAttemptCompleted = TaskAttemptCompleted(
            taskName = message.taskName,
            taskId = message.taskId,
            taskRetryIndex = message.taskRetryIndex,
            taskAttemptId = message.taskAttemptId,
            taskRetrySequence = message.taskRetrySequence,
            taskReturnValue = ReturnValue.from(returnValue),
            taskMeta = taskMeta,
            emitterName = clientName
        )

        sendToTaskEngine(taskAttemptCompleted)
    }
}
