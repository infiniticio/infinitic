package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.worker.data.MethodContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class TaskProxyHandler(private val methodContext: () -> MethodContext) : InvocationHandler {

    /*
     * implements the synchronous processing of a task or child workflow
     */
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? =
        methodContext()
            .dispatch(method, args ?: arrayOf(), method.returnType)
            .result()

    /*
     * provides a proxy instance of type T
     */
    inline fun <reified S> instance() = Proxy.newProxyInstance(
        S::class.java.classLoader,
        arrayOf(S::class.java),
        this
    ) as S
}
