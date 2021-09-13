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

import io.infinitic.annotations.Name
import io.infinitic.exceptions.thisShouldNotHappen
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

sealed class ProxyHandler<T : Any>(
    open val klass: Class<out T>,
    open val dispatcherFn: () -> Dispatcher
) : InvocationHandler {

    companion object {
        @JvmStatic private val invocationType: ThreadLocal<ProxyInvokeType> = ThreadLocal.withInitial { ProxyInvokeType.DISPATCH_SYNC }
        @JvmStatic private val invocationHandler: ThreadLocal<ProxyHandler<*>?> = ThreadLocal()

        fun <R : Any?> async(invoke: () -> R): ProxyHandler<*> {
            invocationType.set(ProxyInvokeType.DISPATCH_ASYNC)
            invoke()
            invocationType.set(ProxyInvokeType.DISPATCH_SYNC)
            val handler = invocationHandler.get()!!
            invocationHandler.set(null)

            return handler
        }
    }

    lateinit var method: Method
    lateinit var args: Array<out Any>

    /**
     * Name provided by @Name annotation, if any
     */
    private val classAnnotatedName: String? by lazy {
        findClassNamePerAnnotation(klass)
    }

    /**
     * Class name provided by @Name annotation, or java class name by default
     */
    protected val className: String by lazy {
        classAnnotatedName ?: klass.name
    }

    /**
     * SimpleName provided by @Name annotation, or class name by default
     */
    protected val simpleName: String by lazy {
        "${classAnnotatedName ?: klass.simpleName}::$methodName"
    }

    /**
     * Method name provided by @Name annotation, or java method name by default
     */
    val methodName: String by lazy {
        findMethodNamePerAnnotation(klass, method) ?: method.name
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

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val any = getAsyncReturnValue(method)

        if (method.declaringClass == Object::class.java) return when (method.name) {
            "toString" -> klass.name
            else -> any
        }

        this.method = method
        this.args = args ?: arrayOf()

        return when (isInvokeSync()) {
            // sync => run directly from dispatcher
            true -> dispatcherFn().dispatchAndWait(this)
            // store current instance to get retrieved from ProxyHandler.async
            false -> { invocationHandler.set(this); any }
        }
    }

    private fun isInvokeSync(): Boolean = when (invocationType.get()) {
        ProxyInvokeType.DISPATCH_SYNC -> true
        ProxyInvokeType.DISPATCH_ASYNC -> false
        null -> thisShouldNotHappen()
    }

    /**
     * Returns a type-compatible value to avoid a java runtime exception
     */
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

    private fun findMethodNamePerAnnotation(klass: Class<*>, method: Method): String? {
        var clazz = klass

        do {
            // has current class a @Name annotation on the targeted method?
            try {
                clazz.getMethod(method.name, *method.parameterTypes)
                    .also { it.isAccessible = true }
                    .getAnnotation(Name::class.java)
                    ?.also { return it.name }
            } catch (e: Exception) {
                // continue
            }

            // has any of the interfaces a @Name annotation on the targeted method?
            clazz.interfaces.forEach { interfaze ->
                findMethodNamePerAnnotation(interfaze, method)?.also { return it }
            }

            // if not, inspect the superclass
            clazz = clazz.superclass ?: break
        } while ("java.lang.Object" != clazz.canonicalName)

        return null
    }

    private fun findClassNamePerAnnotation(klass: Class<*>): String? {
        var clazz = klass

        do {
            // has current class a @Name annotation?
            clazz.getAnnotation(Name::class.java)?.also { return it.name }

            // has any of the interfaces a @Name annotation?
            clazz.interfaces.forEach { interfaze ->
                findClassNamePerAnnotation(interfaze)?.also { return it }
            }

            // if not, inspect the superclass
            clazz = clazz.superclass ?: break
        } while ("java.lang.Object" != clazz.canonicalName)

        return null
    }
}