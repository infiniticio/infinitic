package io.infinitic.worker

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.Constants
import io.infinitic.common.taskManager.avro.AvroConverter
import io.infinitic.common.taskManager.data.TaskAttemptError
import io.infinitic.common.taskManager.data.TaskOutput
import io.infinitic.common.taskManager.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.common.taskManager.exceptions.InvalidUseOfDividerInTaskName
import io.infinitic.common.taskManager.exceptions.MultipleUseOfDividerInTaskName
import io.infinitic.common.taskManager.exceptions.ProcessingTimeout
import io.infinitic.common.taskManager.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.common.taskManager.messages.ForWorkerMessage
import io.infinitic.common.taskManager.messages.RunTask
import io.infinitic.common.taskManager.messages.TaskAttemptCompleted
import io.infinitic.common.taskManager.messages.TaskAttemptFailed
import io.infinitic.common.taskManager.messages.TaskAttemptStarted
import io.infinitic.common.taskManager.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.taskManager.parser.getMethodPerNameAndParameterTypes
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.common.taskManager.Task
import io.infinitic.common.workflowManager.Workflow
import io.infinitic.worker.task.RetryDelay
import io.infinitic.worker.task.RetryDelayFailed
import io.infinitic.worker.task.RetryDelayRetrieved
import io.infinitic.worker.task.TaskAttemptContext
import io.infinitic.worker.task.TaskCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

open class Worker(val dispatcher: Dispatcher) {

    companion object {

        // map taskName <> taskInstance
        private val registeredTasks = ConcurrentHashMap<String, Task>()

        /**
         * Use this to register a task instance to use for a given Task interface
         */
        inline fun <reified T : Task> register(taskInstance: T) = register(T::class.java.name, taskInstance)

        /**
         * Use this to register a task instance to use for a given Task interface
         */
        fun register(taskName: String, taskInstance: Task) {
            if (taskName.contains(Constants.METHOD_DIVIDER)) throw InvalidUseOfDividerInTaskName(taskName)

            registeredTasks[taskName] = taskInstance
        }

        // map workflowName <> workflowImplementation
        private val registeredWorkflows = ConcurrentHashMap<String, Class<out Workflow>>()

        /**
         * Use this to register a workflow definition to use for a given Workflow interface
         */
        inline fun <reified T : Workflow> register(klass: Class<out T>) = register(T::class.java.name, klass)

        /**
         * Use this to register a workflow definition to use for a given Workflow interface
         */
        fun register(workflowName: String, workflowClass: Class<out Workflow>) {
            registeredWorkflows[workflowName] = workflowClass
        }

        /**
         * Use this method to unregister a given name (mostly used in tests)
         */
        inline fun <reified T : Any> unregister() = unregister(T::class.java.name)

        /**
         * Use this method to unregister a given name (mostly used in tests)
         */
        fun unregister(name: String) {
            registeredTasks.remove(name)
            registeredWorkflows.remove(name)
        }
    }

    fun getTaskInstance(name: String) = registeredTasks[name] ?: throw ClassNotFoundDuringInstantiation(name)

    fun getWorkflowClass(name: String) = registeredWorkflows[name] ?: throw ClassNotFoundDuringInstantiation(name)

    suspend fun handle(avro: AvroEnvelopeForWorker) = when (val msg = AvroConverter.fromWorkers(avro)) {
        is RunTask -> runTask(msg)
    }

    suspend fun handle(message: ForWorkerMessage) = when (message) {
        is RunTask -> runTask(message)
    }

    suspend fun runTask(msg: RunTask) {
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
                // we stop here
                return@withContext
            }

            val taskAttemptContext = TaskAttemptContext(
                worker = worker,
                taskId = msg.taskId,
                taskAttemptId = msg.taskAttemptId,
                taskAttemptIndex = msg.taskAttemptIndex,
                taskAttemptRetry = msg.taskAttemptRetry,
                taskMeta = msg.taskMeta,
                taskOptions = msg.taskOptions
            )

            // set taskAttemptContext into task (if a property with right type is present)
            try {
                setTaskContext(task, taskAttemptContext)
            } catch (e: Exception) {
                // returning the exception (no retry)
                sendTaskFailed(msg, e, null)
                // we stop here
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
                // update context with the cause (to be potentially used in getRetryDelay method)
                taskAttemptContext.exception = e.cause
                // retrieve delay before retry
                getRetryDelayAndFailTask(task, msg, taskAttemptContext)
            } catch (e: TimeoutCancellationException) {
                // update context with the cause (to be potentially used in getRetryDelay method)
                taskAttemptContext.exception = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
                // returning a timeout
                getRetryDelayAndFailTask(task, msg, taskAttemptContext)
            } catch (e: Exception) {
                // returning the exception (no retry)
                sendTaskFailed(msg, e, null)
            }
        }
    }

    private suspend fun executeTask(method: Method, task: Any, parameters: Array<out Any?>) = coroutineScope {
        val output = method.invoke(task, *parameters)
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

    private suspend fun getRetryDelayAndFailTask(task: Any, msg: RunTask, context: TaskAttemptContext) {
        when (val delay = getDelayBeforeRetry(task)) {
            is RetryDelayRetrieved -> {
                // returning the original cause
                sendTaskFailed(msg, context.exception, delay.value)
            }
            is RetryDelayFailed -> {
                // returning the error in getRetryDelay, without retry
                sendTaskFailed(msg, delay.e, null)
            }
        }
    }

    private fun parse(msg: RunTask): TaskCommand {
        val (taskName, methodName) = getClassAndMethodName("${msg.taskName}")
        val task = getTaskInstance(taskName)
        val parameterTypes = msg.taskMeta.parameterTypes
        val method = if (parameterTypes == null) {
            getMethodPerNameAndParameterCount(task, methodName, msg.taskInput.size)
        } else {
            getMethodPerNameAndParameterTypes(task, methodName, parameterTypes)
        }

        return TaskCommand(task, method, msg.taskInput.data, msg.taskOptions)
    }

    private fun getClassAndMethodName(name: String): List<String> {
        val parts = name.split(Constants.METHOD_DIVIDER)
        return when (parts.size) {
            1 -> parts + Constants.METHOD_DEFAULT
            2 -> parts
            else -> throw MultipleUseOfDividerInTaskName(name)
        }
    }

    // TODO: currently it's not possible to use class extension to implement a working getRetryDelay() method
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

    private suspend fun sendTaskStarted(msg: RunTask) {
        val taskAttemptStarted = TaskAttemptStarted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskAttemptIndex = msg.taskAttemptIndex
        )

        dispatcher.toTaskEngine(taskAttemptStarted)
    }

    private suspend fun sendTaskFailed(msg: RunTask, error: Throwable?, delay: Float? = null) {
        val taskAttemptFailed = TaskAttemptFailed(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptDelayBeforeRetry = delay,
            taskAttemptError = TaskAttemptError(error)
        )

        dispatcher.toTaskEngine(taskAttemptFailed)
    }

    private suspend fun sendTaskCompleted(msg: RunTask, output: Any?) {
        val taskAttemptCompleted = TaskAttemptCompleted(
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskOutput = TaskOutput(output)
        )

        dispatcher.toTaskEngine(taskAttemptCompleted)
    }
}
