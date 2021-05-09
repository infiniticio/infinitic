/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.proxies

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

open class MethodProxyHandler<T>(protected open val klass: Class<T>) : InvocationHandler {
    var isSync: Boolean = true
    var methods: MutableList<Method> = mutableListOf()
    var args: MutableList<Array<out Any>> = mutableListOf()

    /*
     * invoke method is called when a method is applied to the proxy instance
     */
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (method.name == "toString") return klass.name

        // store method and args
        this.methods.add(method)
        this.args.add(args ?: arrayOf())

        return getAsyncReturnValue(method)
    }

    /*
     * provides a stub of type T
     */
    @Suppress("UNCHECKED_CAST")
    fun stub() = Proxy.newProxyInstance(
        klass.classLoader,
        arrayOf(klass),
        this
    ) as T

    /*
     * Prepare for reuse
     */
    fun reset() {
        isSync = true
        methods = mutableListOf()
        args = mutableListOf()
    }

    private fun getAsyncReturnValue(method: Method) = when (method.returnType.name) {
        "long" -> 0L
        "int" -> 0
        "short" -> 0.toShort()
        "byte" -> 0.toByte()
        "double" -> 0.toDouble()
        "float" -> 0.toFloat()
        "char" -> 0.toChar()
        "boolean" -> false
        else -> null
    }
}
