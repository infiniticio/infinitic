package com.zenaton.jobManager.client

import com.zenaton.jobManager.common.data.Job
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobOptions
import com.zenaton.jobManager.common.exceptions.NoMethodCallAtDispatch
import java.lang.reflect.Proxy

class Client() {
    lateinit var dispatcher: Dispatcher

    inline fun <reified T> dispatch(
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
}
