package com.zenaton.taskManager.client

import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.Job
import com.zenaton.taskManager.common.data.JobId
import com.zenaton.taskManager.common.data.JobInput
import com.zenaton.taskManager.common.data.JobMeta
import com.zenaton.taskManager.common.data.JobName
import com.zenaton.taskManager.common.data.JobOptions
import com.zenaton.taskManager.common.data.JobOutput
import com.zenaton.taskManager.common.exceptions.NoMethodCallAtDispatch
import com.zenaton.taskManager.common.messages.CancelJob
import com.zenaton.taskManager.common.messages.RetryJob
import java.lang.reflect.Proxy

class Client() {
    lateinit var dispatcher: Dispatcher

    fun setAvroDispatcher(avroDispatcher: AvroDispatcher) {
        dispatcher = Dispatcher(avroDispatcher)
    }

    /*
     * Use this method to dispatch a job
     * TODO: using class instance instead of interface is not supported
     */
    inline fun <reified T> dispatchJob(
        options: JobOptions = JobOptions(),
        meta: JobMeta = JobMeta(),
        method: T.() -> Any?
    ): Job {
        // handler will be where the actual job is done
        val handler = ProxyHandler(T::class.java.name, dispatcher, options, meta)

        // get a proxy for T
        val klass = Proxy.newProxyInstance(
            T::class.java.classLoader,
            kotlin.arrayOf(T::class.java),
            handler
        ) as T

        // method call will actually be applied to handler through the proxy
        klass.method()

        // ask
        return handler.getJob() ?: throw NoMethodCallAtDispatch(T::class.java.name)
    }

    /*
     * Use this method to manually retry a job
     * when a non-null parameter is provided, it will supersede current one
     */
    fun retryJob(
        id: String,
        name: JobName? = null,
        input: JobInput? = null,
        options: JobOptions? = null,
        meta: JobMeta? = null
    ) {
        val msg = RetryJob(
            jobId = JobId(id),
            jobName = name,
            jobInput = input,
            jobOptions = options,
            jobMeta = meta
        )
        dispatcher.toJobEngine(msg)
    }

    /*
     * Use this method to manually cancel a job
     */
    fun cancelJob(
        id: String,
        output: Any? = null
    ) {
        val msg = CancelJob(
            jobId = JobId(id),
            jobOutput = JobOutput(SerializedData.from(output))
        )
        dispatcher.toJobEngine(msg)
    }
}
