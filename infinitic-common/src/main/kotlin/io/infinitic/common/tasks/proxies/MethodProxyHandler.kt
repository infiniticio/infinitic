// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

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
