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
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.SendChannel
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.full.isSubclassOf
import io.infinitic.common.proxies.Method as DataMethod

sealed class ProxyHandler<T : Any>(
    open val klass: Class<out T>,
    open val dispatcherFn: () -> ProxyDispatcher
) : InvocationHandler {

    companion object {
        @JvmStatic private val invocationType: ThreadLocal<ProxyInvokeMode> = ThreadLocal.withInitial { ProxyInvokeMode.DISPATCH_SYNC }
        @JvmStatic private val invocationHandler: ThreadLocal<ProxyHandler<*>?> = ThreadLocal()

        fun <R : Any?> async(invoke: () -> R): ProxyHandler<*>? {
            // set async mode
            invocationType.set(ProxyInvokeMode.DISPATCH_ASYNC)
            // call method
            invoke()
            // restore default sync mode
            invocationType.set(ProxyInvokeMode.DISPATCH_SYNC)

            return invocationHandler.get()
        }
    }

    lateinit var method: Method
    lateinit var methodArgs: Array<out Any>

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
     *
     * MUST be a get() as this.methodName can change when reusing instance
     */
    val simpleName: String
        get() = "${classAnnotatedName ?: klass.simpleName}::$methodName"

    /**
     * Method name provided by @Name annotation, or java method name by default
     *
     * MUST be a get() as this.method can change when reusing instance
     */
    val methodName: String
        get() = findMethodNamePerAnnotation(klass, method) ?: method.name

    /**
     * provides a stub of type T
     */
    @Suppress("UNCHECKED_CAST")
    fun stub() = Proxy.newProxyInstance(
        klass.classLoader,
        arrayOf(klass),
        this
    ) as T

    /**
     * provides details of method called
     */
    fun method() = DataMethod(
        MethodName(methodName),
        MethodParameterTypes.from(method),
        MethodParameters.from(method, methodArgs),
    )

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val any = getAsyncReturnValue(method)

        if (method.declaringClass == Object::class.java) return when (method.name) {
            "toString" -> klass.name
            else -> any
        }

        this.method = method
        this.methodArgs = args ?: arrayOf()

        // set current handler
        invocationHandler.set(this)

        return when (isInvokeSync()) {
            // sync => run directly from dispatcher
            true -> dispatcherFn().dispatchAndWait(this)
            // store current instance to get retrieved from ProxyHandler.async
            false -> any
        }
    }

    /**
     * Check if method called was a getter on a SendChannel
     */
    fun isMethodChannel(): Boolean = method.returnType.kotlin.isSubclassOf(SendChannel::class)

    private fun isInvokeSync(): Boolean = when (invocationType.get()) {
        ProxyInvokeMode.DISPATCH_SYNC -> true
        ProxyInvokeMode.DISPATCH_ASYNC -> false
        null -> thisShouldNotHappen()
    }

    // Returns a type-compatible value to avoid an exception at runtime
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
