// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.worker

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.Constants
import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.common.tasks.exceptions.ProcessingTimeout
import io.infinitic.common.tasks.exceptions.RetryDelayHasWrongReturnType
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.tasks.messages.RunTask
import io.infinitic.common.tasks.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.messages.TaskAttemptFailed
import io.infinitic.common.tasks.messages.TaskAttemptStarted
import io.infinitic.common.tasks.parser.getMethodPerNameAndParameterCount
import io.infinitic.common.tasks.parser.getMethodPerNameAndParameterTypes
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.exceptions.TaskUsedAsWorkflow
import io.infinitic.common.workflows.exceptions.WorkflowUsedAsTask
import io.infinitic.worker.task.RetryDelay
import io.infinitic.worker.task.RetryDelayFailed
import io.infinitic.worker.task.RetryDelayRetrieved
import io.infinitic.worker.task.TaskAttemptContext
import io.infinitic.worker.task.TaskCommand
import io.infinitic.worker.workflowTask.WorkflowTaskImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

typealias InstanceFactory = () -> Any

open class Worker(val dispatcher: Dispatcher) {

    // map taskName <> taskInstance
    private val registeredFactories = mutableMapOf<String, InstanceFactory>()

    // per default, WorkflowTask is registered
    init {
        register<WorkflowTask> { WorkflowTaskImpl() }
    }

    /**
     * Register a factory to use for a given name
     */
    inline fun <reified T> register(noinline factory: InstanceFactory) {
        `access$registeredFactories`[T::class.java.name] = factory
    }

    /**
     * Register a factory to use for a given name
     */
    fun register(name: String, factory: () -> Any) {
        registeredFactories[name] = factory
    }

    /**
     * Unregister a given name (mostly used in tests)
     */
    inline fun <reified T> unregister() {
        `access$registeredFactories`.remove(T::class.java.name)
    }

    /**
     * Unregister a given name (mostly used in tests)
     */
    fun unregister(name: String) {
        registeredFactories.remove(name)
    }

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
                println(e.cause?.cause?.stackTraceToString())
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

    fun getWorkflow(name: String): Workflow {
        val instance = getInstance(name)
        if (instance is Workflow) return instance
        else throw TaskUsedAsWorkflow(name, instance::class.qualifiedName!!)
    }

    private fun getTask(name: String): Any {
        val instance = getInstance(name)
        if (instance is Workflow) throw WorkflowUsedAsTask(name, instance::class.qualifiedName!!)
        else return instance
    }

    fun getRegisteredTasks() =
        registeredFactories
            .map { (name, factory) -> name to factory() }
            .filterNot { (_, instance) -> instance is Workflow }
            .map { (name, _) -> name }

    private fun getInstance(name: String) =
        registeredFactories[name]?.let { it() } ?: throw ClassNotFoundDuringInstantiation(name)

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
        val task = getTask("${msg.taskName}")
        val parameterTypes = msg.methodParameterTypes
        val method = if (parameterTypes.types == null) {
            getMethodPerNameAndParameterCount(task, "${msg.methodName}", msg.methodInput.size)
        } else {
            getMethodPerNameAndParameterTypes(task, "${msg.methodName}", parameterTypes.types!!)
        }

        return TaskCommand(task, method, msg.methodInput.data, msg.taskOptions)
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
            taskOutput = MethodOutput(output)
        )

        dispatcher.toTaskEngine(taskAttemptCompleted)
    }

    @PublishedApi
    internal val `access$registeredFactories`: MutableMap<String, InstanceFactory>
        get() = registeredFactories
}
