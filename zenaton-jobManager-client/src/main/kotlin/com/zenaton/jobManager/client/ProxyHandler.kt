package com.zenaton.jobManager.client

import com.fasterxml.jackson.core.JsonProcessingException
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.client.data.Job
import com.zenaton.jobManager.common.Constants
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.exceptions.ErrorDuringJsonDeserializationOfParameter
import com.zenaton.jobManager.common.exceptions.ErrorDuringJsonSerializationOfParameter
import com.zenaton.jobManager.common.exceptions.InconsistentJsonSerializationOfParameter
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
            jobInput = JobInput(args?.mapIndexed { index, value -> getSerializedData(method.parameters[index].name, value, method.parameterTypes[index], method.name, className) } ?: listOf()),
            jobMeta = JobMeta.forParameterTypes(method.parameterTypes.map { it.name })
        )
        dispatcher.toJobEngine(msg)

        return null
    }

    private fun getSerializedData(parameterName: String, parameterValue: Any?, parameterType: Class<*>, methodName: String, className: String): SerializedData {
        val data: SerializedData
        val restoredValue: Any?
        // serialize data
        try {
            data = SerializedData.from(parameterValue)
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonSerializationOfParameter(parameterName, parameterValue, parameterType.name, methodName, className)
        }
        // for user convenience, we check here that data can actually be deserialized
        try {
            restoredValue = data.deserialize()
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonDeserializationOfParameter(parameterName, parameterValue, parameterType.name, methodName, className)
        }
        // check that serialization/deserialization process works as expected
        if (parameterValue != restoredValue) throw InconsistentJsonSerializationOfParameter(parameterName, parameterValue, restoredValue, parameterType.name, methodName, className)

        return data
    }

    fun getJob() = jobId?.let { Job(it) }
}
