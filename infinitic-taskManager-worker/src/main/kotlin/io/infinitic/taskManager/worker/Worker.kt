package io.infinitic.taskManager.worker

import io.infinitic.taskManager.common.Constants
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskAttemptContext
import io.infinitic.taskManager.common.data.TaskAttemptError
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.exceptions.InvalidUseOfDividerInTaskName
import io.infinitic.taskManager.common.exceptions.MultipleUseOfDividerInTaskName
import io.infinitic.taskManager.common.exceptions.ProcessingTimeout
import io.infinitic.taskManager.common.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.messages.TaskAttemptCompleted
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.messages.TaskAttemptStarted
import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterTypes
import io.infinitic.taskManager.common.parser.getMethodPerNameAndParameterCount
import io.infinitic.taskManager.common.parser.getNewInstancePerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

open class Worker {
    lateinit var dispatcher: Dispatcher

    companion object {

        // map taskName <> taskInstance
        private val registeredTasks = ConcurrentHashMap<String, Any>()

        /**
         * Use this method to register the task instance to use for a given name
         */
        fun register(taskName: String, taskInstance: Any) {
            if (taskName.contains(Constants.METHOD_DIVIDER)) throw InvalidUseOfDividerInTaskName(taskName)

            registeredTasks[taskName] = taskInstance
        }

        /**
         * Use this method to unregister a given name (mostly used in tests)
         */
        fun unregister(taskName: String) {
            registeredTasks.remove(taskName)
        }

        /**
         * Use this method to register the task instance to use for a given class
         */
        inline fun <reified T> register(taskInstance: Any) = register(T::class.java.name, taskInstance)

        /**
         * Use this method to unregister a given class (mostly used in tests)
         */
        inline fun <reified T> unregister() = unregister(T::class.java.name)
    }

    fun setAvroDispatcher(avroDispatcher: AvroDispatcher) {
        dispatcher = Dispatcher(avroDispatcher)
    }

    suspend fun handle(avro: AvroEnvelopeForWorker) = when (val msg = AvroConverter.fromWorkers(avro)) {
        is RunTask -> runTask(msg)
    }

    suspend fun runTask(msg: RunTask) = withContext(Dispatchers.Default) {
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
            taskId = msg.taskId,
            taskAttemptId = msg.taskAttemptId,
            taskAttemptIndex = msg.taskAttemptIndex,
            taskAttemptRetry = msg.taskAttemptRetry,
            taskMeta = msg.taskMeta,
            taskOptions = msg.taskOptions
        )

        // set taskAttemptContext into task (if a property with right type is present)
        try {
            setTaskTaskContext(task, taskAttemptContext)
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskFailed(msg, e, null)
            // we stop here
            return@withContext
        }

        val scope = CoroutineScope(coroutineContext + Job())
        // running timeout delay if needed
        if (options.runningTimeout != null && options.runningTimeout!! > 0F) {
            scope.launch {
                delay((1000 * options.runningTimeout!!).toLong())
                ensureActive()
                // update context with the cause (to be potentially used in getRetryDelay method)
                taskAttemptContext.exception = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
                // returning a timeout
                getRetryDelayAndFailTask(task, msg, taskAttemptContext)
                // cancel everything else
                scope.cancel()
            }
        }

        val taskJob = scope.async {
            val output = method.invoke(task, *parameters)
            ensureActive()
            sendTaskCompleted(msg, output)
            // cancel everything else
            scope.cancel()
        }

        try {
            taskJob.await()
        } catch (e: InvocationTargetException) {
            // update context with the cause (to be potentially used in getRetryDelay method)
            taskAttemptContext.exception = e.cause
            // retrieve delay before retry
            getRetryDelayAndFailTask(task, msg, taskAttemptContext)
        } catch (e: CancellationException) {
            // Do nothing as the failure is already reported by the timeout coroutine when cancel happens
        } catch (e: Exception) {
            // returning the exception (no retry)
            sendTaskFailed(msg, e, null)
        } finally {
            scope.coroutineContext[Job]?.join()
        }
    }

    private fun setTaskTaskContext(task: Any, context: TaskAttemptContext) {
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

    private fun getTaskInstance(name: String): Any {
        // return registered instance if any
        if (registeredTasks.containsKey(name)) return registeredTasks[name]!!

        // if no instance is registered, try to instantiate this task
        return getNewInstancePerName(name)
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
