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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.tasks.data.TaskError
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.exceptions.ProcessingTimeout
import io.infinitic.tasks.Task
import io.infinitic.tasks.TaskContext
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.executor.task.RetryDelayFailed
import io.infinitic.tasks.executor.task.RetryDelayRetrieved
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.task.TaskContextImpl
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField

class TaskExecutor(
    sendToTaskEngine: SendToTaskEngine,
    val taskExecutorRegister: TaskExecutorRegister
) : TaskExecutorRegister by taskExecutorRegister {

    private val sendToTaskEngine: (suspend (TaskEngineMessage) -> Unit) =
        { msg: TaskEngineMessage -> sendToTaskEngine(msg, MillisDuration(0)) }

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskExecutorMessage) {
        logger.debug("receiving {}", message)

        when (message) {
            is ExecuteTaskAttempt -> executeTaskAttempt(message)
        }
    }

    private suspend fun executeTaskAttempt(message: ExecuteTaskAttempt) {
        val taskContext = TaskContextImpl(
            register = this,
            id = message.taskId.id,
            attemptId = message.taskAttemptId.id,
            retrySequence = message.taskRetrySequence.int,
            retryIndex = message.taskRetryIndex.int,
            lastError = message.lastTaskError,
            meta = message.taskMeta.map.toMutableMap(),
            options = message.taskOptions
        )

        // trying to instantiate the task
        val (task, method, parameters, options) = try {
            parse(message)
        } catch (e: Throwable) {
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
            val cause = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
            // returning a timeout
            failTaskWithRetry(task, message, cause)
        } catch (e: Throwable) {
            // returning the exception (no retry)
            sendTaskAttemptFailed(message, e, null, TaskMeta(task.context.meta))
        }
    }

    private suspend fun runTask(method: Method, task: Any, parameters: List<Any?>) = coroutineScope {
        val output = method.invoke(task, *parameters.toTypedArray())
        ensureActive()
        output
    }

    private fun setTaskContext(task: Any, context: TaskContext) {
        task::class.memberProperties.find {
            it.returnType.isSubtypeOf(TaskContext::class.starProjectedType)
        }?.javaField?.apply {
            isAccessible = true
            set(task, context)
            // IMPORTANT: visibility must NOT be set back to initial value
            // visibility being static it would lead to race conditions
        }
    }

    private suspend fun failTaskWithRetry(
        task: Task,
        msg: ExecuteTaskAttempt,
        cause: Exception
    ) {
        when (val delay = getDelayBeforeRetry(task, cause)) {
            is RetryDelayRetrieved -> {
                // returning the original cause
                sendTaskAttemptFailed(
                    msg,
                    cause,
                    delay.value?.toSeconds()?.let {
                        MillisDuration((1000F * it).toLong())
                    },
                    TaskMeta(task.context.meta)
                )
            }
            is RetryDelayFailed -> {
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
        val method = if (parameterTypes == null) {
            getMethodPerNameAndParameterCount(task, "${msg.methodName}", msg.methodParameters.size)
        } else {
            getMethodPerNameAndParameterTypes(task, "${msg.methodName}", parameterTypes.types)
        }

        return TaskCommand(task, method, msg.methodParameters.get(), msg.taskOptions)
    }

    private fun getDelayBeforeRetry(task: Task, cause: Exception) = try {
        RetryDelayRetrieved(task.getDurationBeforeRetry(cause))
    } catch (e: Throwable) {
        logger.error("taskId {} - error when executing getDurationBeforeRetry method {}", task.context.id, e)
        RetryDelayFailed(e)
    }

    private suspend fun sendTaskAttemptFailed(
        message: ExecuteTaskAttempt,
        error: Throwable,
        delay: MillisDuration?,
        taskMeta: TaskMeta
    ) {

        logger.error("taskId {} - error {}", message.taskId, error)

        val taskAttemptFailed = TaskAttemptFailed(
            taskId = message.taskId,
            taskName = message.taskName,
            taskAttemptId = message.taskAttemptId,
            taskRetrySequence = message.taskRetrySequence,
            taskRetryIndex = message.taskRetryIndex,
            taskAttemptDelayBeforeRetry = delay,
            taskAttemptError = TaskError.from(error),
            taskMeta = taskMeta
        )

        sendToTaskEngine(taskAttemptFailed)
    }

    private suspend fun sendTaskCompleted(
        message: ExecuteTaskAttempt,
        returnValue: Any?,
        taskMeta: TaskMeta
    ) {
        val taskAttemptCompleted = TaskAttemptCompleted(
            taskId = message.taskId,
            taskName = message.taskName,
            taskAttemptId = message.taskAttemptId,
            taskRetrySequence = message.taskRetrySequence,
            taskRetryIndex = message.taskRetryIndex,
            taskReturnValue = MethodReturnValue.from(returnValue),
            taskMeta = taskMeta
        )

        sendToTaskEngine(taskAttemptCompleted)
    }
}
