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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

abstract class MethodProxyHandler<T>(protected open val klass: Class<T>) : InvocationHandler {
    /**
     * isSync is true, if the last method was called with a synchronous syntax
     */
    var isSync: Boolean = true

    /**
     * history of methods called on this stub
     */
    protected var methods: MutableList<Method> = mutableListOf()

    /**
     * history of arguments of methods called on this stub
     */
    private var args: MutableList<Array<out Any>> = mutableListOf()

    /**
     * Implemented by task, workflow or channel stubs
     */
    abstract val method: Method

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
    val simpleName: String
        get() = "${classAnnotatedName ?: klass.simpleName}::$methodName"

    /**
     * Method name provided by @Name annotation, or java method name by default
     */
    val methodName: String
        get() = findMethodNamePerAnnotation(klass, method) ?: method.name

    val methodArgs: Array<out Any>
        get() {
            // ensure checks are performed
            method

            return args.last()
        }

    /*
     * invoke method is called when a method is applied to the proxy instance
     */
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val default = getAsyncReturnValue(method)

        if (method.declaringClass == Object::class.java) return when (method.name) {
            "toString" -> klass.name
            else -> default
        }

        // store method and args
        this.methods.add(method)
        this.args.add(args ?: arrayOf())

        return default
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

    /**
     * Returns a type-compatible value to avoid a java runtime exception
     */
    protected fun getAsyncReturnValue(method: Method) = when (method.returnType.name) {
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
