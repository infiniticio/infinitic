package com.zenaton.jobManager.client

import com.fasterxml.jackson.core.JsonProcessingException
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.client.data.Job
import com.zenaton.jobManager.common.Constants
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.exceptions.ErrorDuringJsonSerializationOfParameter
import com.zenaton.jobManager.common.exceptions.MultipleMethodCallsAtDispatch
import com.zenaton.jobManager.common.messages.DispatchJob
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyHandler(private val className: String, private val dispatcher: Dispatcher) : InvocationHandler {
    private var jobId: JobId? = null

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (jobId != null) throw MultipleMethodCallsAtDispatch(className)

        jobId = JobId()
        val msg = DispatchJob(
            jobId = jobId!!,
            jobName = JobName("$className${Constants.METHOD_DIVIDER}${method.name}"),
            jobInput = JobInput(args?.mapIndexed { index, value -> getSerializedData(method.parameters[index].name, value, method.parameterTypes[index].name, method.name, className) } ?: listOf()),
            jobMeta = JobMeta(mapOf(Constants.META_PARAMETER_TYPES to SerializedData.from(method.parameterTypes.map { it.name })))
        )
        dispatcher.toJobEngine(msg)

        return null
    }

    private fun getSerializedData(parameterName: String, parameterValue: Any?, parameterType: String, methodName: String, className: String): SerializedData {
        val data: SerializedData
        try {
            data = SerializedData.from(parameterValue)
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonSerializationOfParameter(parameterName, parameterValue, parameterType, methodName, className)
        }
        return data
    }

    fun getJob() = jobId?.let { Job(it) }
}
