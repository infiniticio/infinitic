package io.infinitic.taskManager.client

import io.infinitic.taskManager.common.exceptions.MultipleMethodCallsAtDispatch
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyHandler : InvocationHandler {
    var method: Method? = null
    lateinit var args: Array<out Any>

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (this.method != null) throw MultipleMethodCallsAtDispatch(method.declaringClass.name)

        this.method = method
        this.args = args ?: arrayOf()

        return null
    }
}
