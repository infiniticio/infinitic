package com.zenaton.taskManager.worker
import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.Constants
import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.data.JobAttemptContext
import com.zenaton.taskManager.common.data.JobAttemptError
import com.zenaton.taskManager.common.data.JobInput
import com.zenaton.taskManager.common.data.JobOutput
import com.zenaton.taskManager.common.exceptions.ClassNotFoundDuringJobInstantiation
import com.zenaton.taskManager.common.exceptions.RetryDelayHasWrongReturnType
import com.zenaton.taskManager.common.exceptions.ErrorDuringJobInstantiation
import com.zenaton.taskManager.common.exceptions.ExceptionDuringParametersDeserialization
import com.zenaton.taskManager.common.exceptions.InvalidUseOfDividerInJobName
import com.zenaton.taskManager.common.exceptions.JobAttemptContextRetrievedOutsideOfProcessingThread
import com.zenaton.taskManager.common.exceptions.JobAttemptContextSetFromExistingProcessingThread
import com.zenaton.taskManager.common.exceptions.MultipleUseOfDividerInJobName
import com.zenaton.taskManager.common.exceptions.NoMethodFoundWithParameterCount
import com.zenaton.taskManager.common.exceptions.NoMethodFoundWithParameterTypes
import com.zenaton.taskManager.common.exceptions.ProcessingTimeout
import com.zenaton.taskManager.common.exceptions.TooManyMethodsFoundWithParameterCount
import com.zenaton.taskManager.common.messages.ForWorkerMessage
import com.zenaton.taskManager.common.messages.JobAttemptCompleted
import com.zenaton.taskManager.common.messages.JobAttemptFailed
import com.zenaton.taskManager.common.messages.JobAttemptStarted
import com.zenaton.taskManager.common.messages.RunJob
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

open class Worker {
    lateinit var dispatcher: Dispatcher

    companion object {

        // map jobName <> jobInstance
        private val registeredTasks = ConcurrentHashMap<String, Any>()

        // cf. https://www.baeldung.com/java-threadlocal
        private val contexts = ConcurrentHashMap<Long, JobAttemptContext>()

        private fun getContextKey() = Thread.currentThread().id

        /**
         * Use this to retrieve JobAttemptContext associated to current task
         */
        // TODO: currently running task in coroutines (instead of Thread) is not supported (as Context is mapped to threadId)
        val context: JobAttemptContext
            get() {
                val key = getContextKey()
                if (! contexts.containsKey(key)) throw JobAttemptContextRetrievedOutsideOfProcessingThread()

                return contexts[key]!!
            }

        /**
         * Use this method to register the task instance to use for a given name
         */
        fun register(jobName: String, jobInstance: Any) {
            if (jobName.contains(Constants.METHOD_DIVIDER)) throw InvalidUseOfDividerInJobName(jobName)

            registeredTasks[jobName] = jobInstance
        }

        /**
         * Use this method to unregister a given name (mostly used in tests)
         */
        fun unregister(jobName: String) {
            registeredTasks.remove(jobName)
        }

        /**
         * Use this method to register the task instance to use for a given class
         */
        inline fun <reified T> register(jobInstance: Any) = register(T::class.java.name, jobInstance)

        /**
         * Use this method to unregister a given class (mostly used in tests)
         */
        inline fun <reified T> unregister() = unregister(T::class.java.name)
    }

    fun setAvroDispatcher(avroDispatcher: AvroDispatcher) {
        dispatcher = Dispatcher(avroDispatcher)
    }

    fun handle(msg: AvroEnvelopeForWorker) {
        runBlocking {
            handle(AvroConverter.fromWorkers(msg))
        }
    }

    suspend fun handle(msg: ForWorkerMessage) {
        when (msg) {
            is RunJob -> {
                // let engine know that we are processing the message
                sendTaskStarted(msg)

                // trying to instantiate the task
                val (task, method, parameters, options) = try {
                    parse(msg)
                } catch (e: Exception) {
                    // returning the exception (no retry)
                    sendTaskFailed(msg, e, null)
                    // we stop here
                    return
                }

                val context = JobAttemptContext(
                    jobId = msg.jobId,
                    jobAttemptId = msg.jobAttemptId,
                    jobAttemptIndex = msg.jobAttemptIndex,
                    jobAttemptRetry = msg.jobAttemptRetry,
                    jobMeta = msg.jobMeta,
                    jobOptions = msg.jobOptions
                )

                // the method invocation is done through a coroutine
                // - to manage processing timeout
                // - to manage cancellation after timeout
                val parentJob = Job()
                CoroutineScope(Dispatchers.Default + parentJob).launch {
                    var contextKey = 0L

                    launch {
                        try {
                            // ensure that contextKey is linked to same thread than method.invoke below
                            contextKey = getContextKey()

                            // add context to static list
                            setContext(contextKey, context)

                            // running timeout delay if any
                            if (options.runningTimeout != null && options.runningTimeout!! > 0F) {
                                launch {
                                    delay((1000 * options.runningTimeout!!).toLong())
                                    // update context with the cause (to be potentially used in getRetryDelay method)
                                    context.exception = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
                                    // returning a timeout
                                    getRetryDelayAndFailTask(task, msg, parentJob, contextKey, context)
                                }
                            }

                            val output = method.invoke(task, *parameters)
                            // isActive below checks that the coroutine has not been canceled by timeout
                            if (isActive) {
                                completeTask(msg, output, parentJob, contextKey)
                            }
                        } catch (e: InvocationTargetException) {
                            if (isActive) {
                                // update context with the cause (to be potentially used in getRetryDelay method)
                                context.exception = e.cause
                                // retrieve delay before retry
                                getRetryDelayAndFailTask(task, msg, parentJob, contextKey, context)
                            }
                        } catch (e: Exception) {
                            if (isActive) {
                                // returning the exception (no retry)
                                failTask(msg, e, null, parentJob, contextKey)
                            }
                        }
                    }
                }.join()
            }
        }
    }

    private fun setContext(key: Long, context: JobAttemptContext) {
        if (contexts.containsKey(key)) throw JobAttemptContextSetFromExistingProcessingThread()
        contexts[key] = context
    }

    private fun delContext(key: Long) {
        if (! contexts.containsKey(key)) throw JobAttemptContextSetFromExistingProcessingThread()
        contexts.remove(key)
    }

    private fun getRetryDelayAndFailTask(task: Any, msg: RunJob, parentJob: Job, contextKey: Long, context: JobAttemptContext) {
        when (val delay = getDelayBeforeRetry(task, context)) {
            is RetryDelayRetrieved -> {
                println("delay=$delay")
                // returning the original cause
                failTask(msg, context.exception, delay.value, parentJob, contextKey)
            }
            is RetryDelayFailed -> {
                println("delay=$delay")
                // returning the error in getRetryDelay, without retry
                failTask(msg, delay.e, null, parentJob, contextKey)
            }
        }
    }

    private fun completeTask(msg: RunJob, output: Any?, parentJob: Job, contextKey: Long) {
        // returning output
        sendJobCompleted(msg, output)

        // removing context from static list
        delContext(contextKey)

        // make sure to close both coroutines
        parentJob.cancel()
    }

    private fun failTask(msg: RunJob, e: Throwable?, delay: Float?, parentJob: Job, contextKey: Long) {
        // returning throwable
        sendTaskFailed(msg, e, delay)

        // removing context from static list
        delContext(contextKey)

        // make sure to close both coroutines
        parentJob.cancel()
    }

    private fun parse(msg: RunJob): JobCommand {
        val (jobName, methodName) = getClassAndMethodNames(msg)
        val job = getTaskInstance(jobName)
        val parameterTypes = getMetaParameterTypes(msg)
        val method = getMethod(job, methodName, msg.jobInput.input.size, parameterTypes)
        val parameters = getParameters(job::class.java.name, methodName, msg.jobInput, parameterTypes ?: method.parameterTypes)

        return JobCommand(job, method, parameters, msg.jobOptions)
    }

    private fun getClassAndMethodNames(msg: RunJob): List<String> {
        val parts = msg.jobName.name.split(Constants.METHOD_DIVIDER)
        return when (parts.size) {
            1 -> parts + Constants.METHOD_DEFAULT
            2 -> parts
            else -> throw MultipleUseOfDividerInJobName(msg.jobName.name)
        }
    }

    private fun getTaskInstance(name: String): Any {
        // return registered instance if any
        if (registeredTasks.containsKey(name)) return registeredTasks[name]!!

        // if no instance is registered, try to instantiate this job
        val klass = getClass(name)

        return try {
            klass.newInstance()
        } catch (e: Exception) {
            throw ErrorDuringJobInstantiation(name)
        }
    }

    private fun getMetaParameterTypes(msg: RunJob) = msg.jobMeta.getParameterTypes()
        ?.map { getClass(it) }
        ?.toTypedArray()

    private fun getClass(name: String) = when (name) {
        "bytes" -> Byte::class.java
        "short" -> Short::class.java
        "int" -> Int::class.java
        "long" -> Long::class.java
        "float" -> Float::class.java
        "double" -> Double::class.java
        "boolean" -> Boolean::class.java
        "char" -> Character::class.java
        else ->
            try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                throw ClassNotFoundDuringJobInstantiation(name)
            }
    }

    // TODO: currently method using "suspend" keyword are not supported
    private fun getMethod(job: Any, methodName: String, parameterCount: Int, parameterTypes: Array<Class<*>>?): Method {
        // Case where parameter types have been provided
        if (parameterTypes != null) return try {
            job::class.java.getMethod(methodName, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            throw NoMethodFoundWithParameterTypes(job::class.java.name, methodName, parameterTypes.map { it.name })
        }

        // if not, hopefully there is only one method with this name
        val methods = job::class.javaObjectType.methods.filter { it.name == methodName && it.parameterCount == parameterCount }
        if (methods.isEmpty()) throw NoMethodFoundWithParameterCount(job::class.java.name, methodName, parameterCount)
        if (methods.size > 1) throw TooManyMethodsFoundWithParameterCount(job::class.java.name, methodName, parameterCount)

        return methods[0]
    }

    private fun getParameters(jobName: String, methodName: String, input: JobInput, parameterTypes: Array<Class<*>>): Array<Any?> {
        return try {
            input.input.mapIndexed {
                index, serializedData ->
                serializedData.deserialize(parameterTypes[index])
            }.toTypedArray()
        } catch (e: Exception) {
            throw ExceptionDuringParametersDeserialization(jobName, methodName, input.input, parameterTypes.map { it.name })
        }
    }

    // TODO: currently it's not possible to use class extension to implement a working getRetryDelay() method
    private fun getDelayBeforeRetry(job: Any, context: JobAttemptContext): RetryDelay {
        val method = try {
            job::class.java.getMethod(Constants.DELAY_BEFORE_RETRY_METHOD, JobAttemptContext::class.java)
        } catch (e: NoSuchMethodException) {
            return RetryDelayRetrieved(null)
        }

        val actualType = method.genericReturnType.typeName
        val expectedType = Float::class.javaObjectType.name
        if (actualType != expectedType) return RetryDelayFailed(
            RetryDelayHasWrongReturnType(job::class.java.name, actualType, expectedType)
        )

        return try {
            RetryDelayRetrieved(method.invoke(job, context) as Float?)
        } catch (e: InvocationTargetException) {
            RetryDelayFailed(e.cause)
        }
    }

    private fun sendTaskStarted(msg: RunJob) {
        val jobAttemptStarted = JobAttemptStarted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex
        )

        dispatcher.toJobEngine(jobAttemptStarted)
    }

    private fun sendTaskFailed(msg: RunJob, error: Throwable?, delay: Float? = null) {
        val jobAttemptFailed = JobAttemptFailed(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptDelayBeforeRetry = delay,
            jobAttemptError = JobAttemptError(error)
        )

        dispatcher.toJobEngine(jobAttemptFailed)
    }

    private fun sendJobCompleted(msg: RunJob, output: Any?) {
        val jobAttemptCompleted = JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobOutput = JobOutput(SerializedData.from(output))
        )

        dispatcher.toJobEngine(jobAttemptCompleted)
    }
}
