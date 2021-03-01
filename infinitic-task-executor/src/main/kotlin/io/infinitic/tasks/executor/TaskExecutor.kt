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
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.tasks.Constants
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskAttemptStarted
import io.infinitic.common.tasks.exceptions.ProcessingTimeout
import io.infinitic.common.tasks.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.common.tasks.executors.messages.CancelTaskAttempt
import io.infinitic.common.tasks.executors.messages.ExecuteTaskAttempt
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.tasks.executor.register.TaskExecutorRegister
import io.infinitic.tasks.executor.task.RetryDelay
import io.infinitic.tasks.executor.task.RetryDelayFailed
import io.infinitic.tasks.executor.task.RetryDelayRetrieved
import io.infinitic.tasks.executor.task.TaskAttemptContext
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.transport.TaskExecutorOutput
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

class TaskExecutor(
    val taskExecutorOutput: TaskExecutorOutput,
    val taskExecutorRegister: TaskExecutorRegister
) : TaskExecutorRegister by taskExecutorRegister {

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun handle(message: TaskExecutorMessage) {
        logger.debug("receiving {} (messageId {})", message, message.messageId)

        when (message) {
            is ExecuteTaskAttempt -> executeTaskAttempt(message)
            is CancelTaskAttempt -> cancelTaskAttempt(message)
        }
    }
    private fun cancelTaskAttempt(message: CancelTaskAttempt) {
        TODO()
    }

    private suspend fun executeTaskAttempt(message: ExecuteTaskAttempt) {
        val taskAttemptContext = TaskAttemptContext(
            taskExecutor = this,
            taskId = "${message.taskId}",
            taskAttemptId = "${message.taskAttemptId}",
            taskRetry = message.taskRetry.int,
            taskAttemptRetry = message.taskAttemptRetry.int,
            previousTaskAttemptError = message.previousTaskAttemptError?.get(),
            taskMeta = message.taskMeta.get(),
            taskOptions = message.taskOptions
        )

        // let engine know that we are processing the message
        sendTaskStarted(message)

        // trying to instantiate the task
        val (task, method, parameters, options) = try {
            parse(message)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskAttemptFailed(message, e, null)
            // stop here
            return
        }

        // set taskAttemptContext into task (if a property with right type is present)
        try {
            setTaskContext(task, taskAttemptContext)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskAttemptFailed(message, e, null)
            // stop here
            return
        }

        try {
            val output = if (options.runningTimeout != null && options.runningTimeout!! > 0F) {
                withTimeout((1000 * options.runningTimeout!!).toLong()) {
                    runTask(method, task, parameters)
                }
            } else {
                runTask(method, task, parameters)
            }
            sendTaskCompleted(message, output)
        } catch (e: InvocationTargetException) {
            // update context with the cause (to be potentially used in getRetryDelay method)
            taskAttemptContext.currentTaskAttemptError = e.cause
            // retrieve delay before retry
            getRetryDelayAndFailTask(task, message, taskAttemptContext)
        } catch (e: TimeoutCancellationException) {
            // update context with the cause (to be potentially used in getRetryDelay method)
            taskAttemptContext.currentTaskAttemptError = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
            // returning a timeout
            getRetryDelayAndFailTask(task, message, taskAttemptContext)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskAttemptFailed(message, e, null)
        }
    }

    private suspend fun runTask(method: Method, task: Any, parameters: List<Any?>) = coroutineScope {
        val output = method.invoke(task, *parameters.toTypedArray())
        ensureActive()
        output
    }

    private fun setTaskContext(task: Any, context: TaskAttemptContext) {
        task::class.memberProperties.find {
            it.returnType.javaType.typeName == TaskAttemptContext::class.java.name
        }?.javaField?.apply {
            isAccessible = true
            set(task, context)
            // IMPORTANT: visibility must NOT be set back to initial value
            // visibility being static it would lead to race conditions
        }
    }

    private suspend fun getRetryDelayAndFailTask(task: Any, msg: ExecuteTaskAttempt, context: TaskAttemptContext) {
        when (val delay = getDelayBeforeRetry(task, msg.taskId)) {
            is RetryDelayRetrieved -> {
                // returning the original cause
                sendTaskAttemptFailed(
                    msg,
                    context.currentTaskAttemptError,
                    delay.value?.let {
                        MillisDuration((1000F * it).toLong())
                    }
                )
            }
            is RetryDelayFailed -> {
                // returning the error in getRetryDelay, without retry
                sendTaskAttemptFailed(msg, delay.e, null)
            }
        }
    }

    private fun parse(msg: ExecuteTaskAttempt): TaskCommand {
        val task = getTaskInstance("${msg.taskName}")

        val parameterTypes = msg.methodParameterTypes
        val method = if (parameterTypes == null) {
            getMethodPerNameAndParameterCount(task, "${msg.methodName}", msg.methodInput.size)
        } else {
            getMethodPerNameAndParameterTypes(task, "${msg.methodName}", parameterTypes.types)
        }

        return TaskCommand(task, method, msg.methodInput.get(), msg.taskOptions)
    }

    private fun getDelayBeforeRetry(task: Any, taskId: TaskId): RetryDelay {
        val method = try {
            task::class.java.getMethod(Constants.DELAY_BEFORE_RETRY_METHOD)
        } catch (e: NoSuchMethodException) {
            logger.info("taskId {} - no ${Constants.DELAY_BEFORE_RETRY_METHOD} method", taskId)
            return RetryDelayRetrieved(null)
        }

        val value = try {
            method.invoke(task)
        } catch (e: InvocationTargetException) {
            logger.error("taskId {} - error when executing ${Constants.DELAY_BEFORE_RETRY_METHOD} method", taskId, e.cause)
            return RetryDelayFailed(e.cause)
        }

        return try {
            RetryDelayRetrieved(value as Float?)
        } catch (e: Exception) {
            logger.error("taskId {} - wrong return type ({}) of ${Constants.DELAY_BEFORE_RETRY_METHOD} method", taskId, method.genericReturnType.typeName)
            return RetryDelayFailed(
                RetryDelayHasWrongReturnType(task::class.java.name, method.genericReturnType.typeName, Float::class.javaObjectType.name)
            )
        }
    }

    private suspend fun sendTaskStarted(message: ExecuteTaskAttempt) {
        val taskAttemptStarted = TaskAttemptStarted(
            taskName = message.taskName,
            taskId = message.taskId,
            taskRetry = message.taskRetry,
            taskAttemptId = message.taskAttemptId,
            taskAttemptRetry = message.taskAttemptRetry
        )

        taskExecutorOutput.sendToTaskEngine(message.messageId, taskAttemptStarted, MillisDuration(0))
    }

    private suspend fun sendTaskAttemptFailed(message: ExecuteTaskAttempt, error: Throwable?, delay: MillisDuration? = null) {
        logger.error("taskId {} - error {}", message.taskId, error)

        val taskAttemptFailed = TaskAttemptFailed(
            taskId = message.taskId,
            taskName = message.taskName,
            taskAttemptId = message.taskAttemptId,
            taskAttemptRetry = message.taskAttemptRetry,
            taskRetry = message.taskRetry,
            taskAttemptDelayBeforeRetry = delay,
            taskAttemptError = TaskAttemptError.from(error)
        )

        taskExecutorOutput.sendToTaskEngine(message.messageId, taskAttemptFailed, MillisDuration(0))
    }

    private suspend fun sendTaskCompleted(message: ExecuteTaskAttempt, output: Any?) {
        val taskAttemptCompleted = TaskAttemptCompleted(
            taskName = message.taskName,
            taskId = message.taskId,
            taskAttemptId = message.taskAttemptId,
            taskAttemptRetry = message.taskAttemptRetry,
            taskRetry = message.taskRetry,
            taskOutput = MethodOutput.from(output)
        )

        taskExecutorOutput.sendToTaskEngine(message.messageId, taskAttemptCompleted, MillisDuration(0))
    }
}
