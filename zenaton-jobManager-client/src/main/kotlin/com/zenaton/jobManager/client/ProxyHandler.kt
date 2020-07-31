package com.zenaton.jobManager.client

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.client.data.Job
import com.zenaton.jobManager.common.Constants
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.messages.DispatchJob
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyHandler(private val className: String, private val dispatcher: Dispatcher) : InvocationHandler {
    private var jobId: JobId? = null

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (jobId != null) throw Exception("only one method of $className can be dispatched at a time")

        jobId = JobId()
        val msg = DispatchJob(
            jobId = jobId!!,
            jobName = JobName("$className${Constants.METHOD_DIVIDER}${method.name}"),
            jobInput = JobInput(args?.map { SerializedData.from(it) } ?: listOf()),
            jobMeta = JobMeta(mapOf(Constants.META_PARAMETER_TYPES to SerializedData.from(method.parameterTypes.map { it.name })))
        )
        dispatcher.toJobEngine(msg)

        return null
    }

    fun getJob() = jobId?.let { Job(it) }
}
