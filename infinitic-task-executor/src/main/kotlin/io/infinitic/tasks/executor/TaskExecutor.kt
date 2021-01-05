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

import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.common.tasks.Constants
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.engine.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.engine.messages.TaskAttemptFailed
import io.infinitic.common.tasks.engine.messages.TaskAttemptStarted
import io.infinitic.common.tasks.exceptions.ProcessingTimeout
import io.infinitic.common.tasks.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.tasks.executor.register.InstanceFactory
import io.infinitic.tasks.executor.register.TaskExecutorRegister
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.executor.task.RetryDelay
import io.infinitic.tasks.executor.task.RetryDelayFailed
import io.infinitic.tasks.executor.task.RetryDelayRetrieved
import io.infinitic.tasks.executor.task.TaskAttemptContext
import io.infinitic.tasks.executor.task.TaskCommand
import io.infinitic.tasks.executor.transport.TaskExecutorOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

open class TaskExecutor(
    open val taskExecutorOutput: TaskExecutorOutput,
    val taskExecutorRegister: TaskExecutorRegister = TaskExecutorRegisterImpl()
) : TaskExecutorRegister by taskExecutorRegister {

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    /**
     * Register a factory to use for a given name
     */
    inline fun <reified T> register(noinline factory: InstanceFactory) {
        taskExecutorRegister.register(T::class.java.name, factory)
    }

    /**
     * Unregister a given name (mostly used in tests)
     */
    inline fun <reified T> unregister() {
        taskExecutorRegister.unregister(T::class.java.name)
    }

    suspend fun handle(msg: TaskExecutorMessage) {
        val worker = this

        withContext(Dispatchers.Default) {
            // let engine know that we are processing the message
            sendTaskStarted(msg)

            // trying to instantiate the task
            val (task, method, parameters, options) = try {
                parse(msg)
            } catch (e: Exception) {
                // returning the exception (no retry)
                sendTaskFailed(msg, e, null)
                // stop here
                return@withContext
            }

            val taskAttemptContext = TaskAttemptContext(
                taskExecutor = worker,
                taskId = "${msg.taskId}",
                taskAttemptId = "${msg.taskAttemptId}",
                taskRetry = msg.taskRetry.int,
                taskAttemptRetry = msg.taskAttemptRetry.int,
                lastTaskAttemptError = msg.lastTaskAttemptError?.get(),
                taskMeta = msg.taskMeta.get(),
                taskOptions = msg.taskOptions
            )

            // set taskAttemptContext into task (if a property with right type is present)
            try {
                setTaskContext(task, taskAttemptContext)
            } catch (e: Exception) {
                // returning the exception (no retry)
                sendTaskFailed(msg, e, null)
                // stop here
                return@withContext
            }

            try {
                val output = if (options.runningTimeout != null && options.runningTimeout!! > 0F) {
                    withTimeout((1000 * options.runningTimeout!!).toLong()) {
                        executeTask(method, task, parameters)
                    }
                } else {
                    executeTask(method, task, parameters)
                }
                sendTaskCompleted(msg, output)
            } catch (e: InvocationTargetException) {
//                println(e.cause?.cause?.stackTraceToString())
                // update context with the cause (to be potentially used in getRetryDelay method)
                taskAttemptContext.currentTaskAttemptError = e.cause
                // retrieve delay before retry
                getRetryDelayAndFailTask(task, msg, taskAttemptContext)
            } catch (e: TimeoutCancellationException) {
                // update context with the cause (to be potentially used in getRetryDelay method)
                taskAttemptContext.currentTaskAttemptError = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
                // returning a timeout
                getRetryDelayAndFailTask(task, msg, taskAttemptContext)
            } catch (e: Exception) {
                // returning the exception (no retry)
                sendTaskFailed(msg, e, null)
            }
        }
    }

    private suspend fun executeTask(method: Method, task: Any, parameters: List<Any?>) = coroutineScope {
        val output = method.invoke(task, *parameters.toTypedArray())
        ensureActive()
        output
    }

    private fun setTaskContext(task: Any, context: TaskAttemptContext) {
        task::class.memberProperties.find {
            it.returnType.javaType.typeName == TaskAttemptContext::class.java.name
        }?.javaField?.apply {
            val accessible = isAccessible
            isAccessible = true
            set(task, context)
            isAccessible = accessible
        }
    }

    private suspend fun getRetryDelayAndFailTask(task: Any, msg: TaskExecutorMessage, context: TaskAttemptContext) {
        when (val delay = getDelayBeforeRetry(task)) {
            is RetryDelayRetrieved -> {
                // returning the original cause
                sendTaskFailed(msg, context.currentTaskAttemptError, delay.value)
            }
            is RetryDelayFailed -> {
                // returning the error in getRetryDelay, without retry
                sendTaskFailed(msg, delay.e, null)
            }
        }
    }

    private fun parse(msg: TaskExecutorMessage): TaskCommand {
        val task = getTaskInstance("${msg.taskName}")

        val parameterTypes = msg.methodParameterTypes
        val method = if (parameterTypes == null) {
            getMethodPerNameAndParameterCount(task, "${msg.methodName}", msg.methodInput.size)
        } else {
            getMethodPerNameAndParameterTypes(task, "${msg.methodName}", parameterTypes.types)
        }

        return TaskCommand(task, method, msg.methodInput.get(), msg.taskOptions)
    }

    private fun getDelayBeforeRetry(task: Any): RetryDelay {
        val method = try {
            task::class.java.getMethod(Constants.DELAY_BEFORE_RETRY_METHOD)
        } catch (e: NoSuchMethodException) {
            return RetryDelayRetrieved(null)
        }

        val actualType = method.genericReturnType.typeName
        val expectedType = Float::class.javaObjectType.name
        if (actualType != expectedType) return RetryDelayFailed(
            RetryDelayHasWrongReturnType(task::class.java.name, actualType, expectedType)
        )

        return try {
            RetryDelayRetrieved(method.invoke(task) as Float?)
        } catch (e: InvocationTargetException) {
            RetryDelayFailed(e.cause)
        }
    }

    private suspend fun sendTaskStarted(msg: TaskExecutorMessage) {
        val taskAttemptStarted = TaskAttemptStarted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskRetry = msg.taskRetry
        )

        taskExecutorOutput.sendToTaskEngine(taskAttemptStarted, 0F)
    }

    private suspend fun sendTaskFailed(msg: TaskExecutorMessage, error: Throwable?, delay: Float? = null) {
        logger.debug("taskId {} - error {}", msg.taskId, error)

        val taskAttemptFailed = TaskAttemptFailed(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskRetry = msg.taskRetry,
            taskAttemptDelayBeforeRetry = delay,
            taskAttemptError = TaskAttemptError.from(error)
        )

        taskExecutorOutput.sendToTaskEngine(taskAttemptFailed, 0F)
    }

    private suspend fun sendTaskCompleted(msg: TaskExecutorMessage, output: Any?) {
        val taskAttemptCompleted = TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskRetry = msg.taskRetry,
            taskOutput = MethodOutput.from(output)
        )

        taskExecutorOutput.sendToTaskEngine(taskAttemptCompleted, 0F)
    }
}
