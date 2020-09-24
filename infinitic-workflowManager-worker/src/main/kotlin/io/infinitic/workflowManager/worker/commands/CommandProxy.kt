package io.infinitic.workflowManager.worker.commands

import io.infinitic.workflowManager.worker.data.MethodRunContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class CommandProxy(private val methodRunContext: () -> MethodRunContext) : InvocationHandler {

    /*
     * implements the synchronous processing of a task or child workflow
     */
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? =
        methodRunContext()
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
