package com.zenaton.jobManager.client

import com.zenaton.jobManager.client.data.Job
import java.lang.Exception
import java.lang.reflect.Proxy

class Client() {
    lateinit var dispatcher: Dispatcher

    inline fun <reified T> dispatch(method: T.() -> Any): Job {
        // handler will be where the actual job is done
        val handler = ProxyHandler(T::class.java.name, dispatcher)

        // get a proxy for T
        val klass = Proxy.newProxyInstance(
            T::class.java.classLoader,
            kotlin.arrayOf(T::class.java),
            handler
        ) as T

        // method call will actually be applied to handler through the proxy
        klass.method()

        // ask
        return handler.getJob() ?: throw Exception("When dispatching, you did not provide a method call")
    }
}
