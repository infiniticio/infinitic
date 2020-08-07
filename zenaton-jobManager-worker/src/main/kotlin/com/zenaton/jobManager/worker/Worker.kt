package com.zenaton.jobManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.Constants
import com.zenaton.jobManager.common.data.JobAttemptError
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobOutput
import com.zenaton.jobManager.common.exceptions.ClassNotFoundDuringJobInstantiation
import com.zenaton.jobManager.common.exceptions.ErrorDuringJobInstantiation
import com.zenaton.jobManager.common.exceptions.InvalidUseOfDividerInJobName
import com.zenaton.jobManager.common.exceptions.MultipleUseOfDividerInJobName
import com.zenaton.jobManager.common.exceptions.NoMethodFoundWithParameterCount
import com.zenaton.jobManager.common.exceptions.NoMethodFoundWithParameterTypes
import com.zenaton.jobManager.common.exceptions.TooManyMethodsFoundWithParameterCount
import com.zenaton.jobManager.common.messages.ForWorkerMessage
import com.zenaton.jobManager.common.messages.JobAttemptCompleted
import com.zenaton.jobManager.common.messages.JobAttemptFailed
import com.zenaton.jobManager.common.messages.JobAttemptStarted
import com.zenaton.jobManager.common.messages.RunJob
import java.lang.reflect.Method

open class Worker {
    lateinit var dispatcher: Dispatcher

    private val registeredJobs = mutableMapOf<String, Any>()

    /**
     * With this method, user can register a job instance to use for a given name
     */
    @Suppress("unused")
    fun register(name: String, job: Any): Worker {
        if (name.contains(Constants.METHOD_DIVIDER)) throw InvalidUseOfDividerInJobName(name)

        registeredJobs[name] = job

        return this
    }

    fun handle(msg: ForWorkerMessage) {
        when (msg) {
            is RunJob -> {
                sendJobStarted(msg)
                try {
                    sendJobCompleted(msg, run(msg))
                } catch (e: Exception) {
                    sendJobFailed(msg, e)
                }
            }
        }
    }

    private fun run(msg: RunJob): Any? {
        val (jobName, methodName) = getClassAndMethodNames(msg)
        val job = getJob(jobName)
        val parameterTypes = getMetaParameterTypes(msg)
        val method = getMethod(job, methodName, msg.jobInput.input.size, parameterTypes)
        val parameters = getParameters(msg.jobInput, parameterTypes ?: method.parameterTypes)

        return method.invoke(job, *parameters)
    }

    private fun getClassAndMethodNames(msg: RunJob): List<String> {
        val parts = msg.jobName.name.split(Constants.METHOD_DIVIDER)
        return when (parts.size) {
            1 -> parts + Constants.METHOD_DEFAULT
            2 -> parts
            else -> throw MultipleUseOfDividerInJobName(msg.jobName.name)
        }
    }

    private fun getJob(name: String): Any {
        // return registered instance if any
        if (registeredJobs.containsKey(name)) return registeredJobs[name]!!

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

    private fun getParameters(input: JobInput, parameterTypes: Array<Class<*>>): Array<Any?> {
        return input.input.mapIndexed {
            index, serializedData ->
            serializedData.deserialize(parameterTypes[index])
        }.toTypedArray()
    }

    private fun sendJobStarted(msg: RunJob) {
        val jobAttemptStarted = JobAttemptStarted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex
        )

        dispatcher.toJobEngine(jobAttemptStarted)
    }

    private fun sendJobFailed(msg: RunJob, error: Exception, delay: Float? = null) {
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
