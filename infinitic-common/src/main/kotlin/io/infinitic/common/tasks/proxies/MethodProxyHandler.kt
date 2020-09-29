package io.infinitic.common.tasks.proxies

import io.infinitic.common.tasks.exceptions.MultipleMethodCallsAtDispatch
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MethodProxyHandler<T>(private val klass: Class<T>) : InvocationHandler {
    var method: Method? = null
    lateinit var args: Array<out Any>

    /*
     * invoke method is called when a method is applied to the proxy instance
     */
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        // toString method is used to retrieve initial class
        if (method.name == "toString") return klass.name

        // invoke should called only once per ProxyHandler instance
        if (this.method != null) throw MultipleMethodCallsAtDispatch(method.declaringClass.name, this.method!!.name, method.name)

        // methods and args are stored for later use
        this.method = method
        this.args = args ?: arrayOf()

        // explicit cast needed for all primitive types
        return when (method.returnType.name) {
            "long" -> 0L
            "int" -> 0.toInt()
            "short" -> 0.toShort()
            "byte" -> 0.toByte()
            "double" -> 0.toDouble()
            "float" -> 0.toFloat()
            "char" -> 0.toChar()
            "boolean" -> false
            else -> null
        }
    }

    /*
     * provides a proxy instance of type T
     */
    @Suppress("UNCHECKED_CAST")
    fun instance() = Proxy.newProxyInstance(
        klass.classLoader,
        kotlin.arrayOf(klass),
        this
    ) as T
}
