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

package io.infinitic.common.parser

import io.infinitic.annotations.Name
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterCountException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterCountException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterTypesException
import java.lang.reflect.Method

// TODO: support methods with varargs
fun getMethodPerNameAndParameters(
    klass: Class<*>,
    name: String,
    parameterTypes: List<String>?,
    parametersCount: Int
): Method = when (parameterTypes) {
    null -> getMethodPerAnnotationAndParametersCount(klass, name, parametersCount)
        ?: getMethodPerNameAndParameterCount(klass, name, parametersCount)
        ?: throw NoMethodFoundWithParameterCountException(klass.name, name, parametersCount)
    else -> getMethodPerAnnotationAndParameterTypes(klass, name, parameterTypes)
        ?: getMethodPerNameAndParameterTypes(klass, name, parameterTypes)
        ?: throw NoMethodFoundWithParameterTypesException(klass.name, name, parameterTypes)
}

fun classForName(name: String): Class<out Any> = when (name) {
    "long" -> Long::class.java
    "int" -> Int::class.java
    "short" -> Short::class.java
    "byte" -> Byte::class.java
    "double" -> Double::class.java
    "float" -> Float::class.java
    "char" -> Char::class.java
    "boolean" -> Boolean::class.java
    else -> Class.forName(name)
}

private fun getMethodPerAnnotationAndParameterTypes(klass: Class<*>, name: String, parameterTypes: List<String>): Method? {
    var clazz = klass

    do {
        // has current class a method with @Name annotation and right parameters?
        val methods = clazz.methods.filter { method ->
            method.isAccessible = true
            method.isAnnotationPresent(Name::class.java) && method.parameterTypes.map { it.name } == parameterTypes
        }
        when (methods.size) {
            0 -> Unit
            1 -> return methods[0]
            else -> throw TooManyMethodsFoundWithParameterTypesException(klass.name, name, parameterTypes)
        }

        // has any of the interfaces a method with @Name annotation and right parameters?
        clazz.interfaces.forEach { interfaze ->
            getMethodPerAnnotationAndParameterTypes(interfaze, name, parameterTypes)?.also { return it }
        }

        // if not, inspect the superclass
        clazz = clazz.superclass ?: break
    } while ("java.lang.Object" != clazz.canonicalName)

    return null
}

private fun getMethodPerNameAndParameterTypes(klass: Class<*>, name: String, parameterTypes: List<String>): Method? = try {
    klass.getMethod(name, *(parameterTypes.map { classForName(it) }.toTypedArray()))
} catch (e: NoSuchMethodException) {
    null
}

private fun getMethodPerAnnotationAndParametersCount(klass: Class<*>, name: String, parameterCount: Int): Method? {
    var clazz = klass

    do {
        // has current class a method with @Name annotation and right count of parameters?
        val methods = clazz.methods.filter { method ->
            method.isAccessible = true
            method.isAnnotationPresent(Name::class.java) && method.parameterTypes.size == parameterCount
        }

        when (methods.size) {
            0 -> Unit
            1 -> return methods[0]
            else -> throw TooManyMethodsFoundWithParameterCountException(klass.name, name, parameterCount)
        }

        // has any of the interfaces a method with @Name annotation and right count of parameters?
        clazz.interfaces.forEach { interfaze ->
            getMethodPerAnnotationAndParametersCount(interfaze, name, parameterCount)?.also { return it }
        }

        // if not, inspect the superclass
        clazz = clazz.superclass ?: break
    } while ("java.lang.Object" != clazz.canonicalName)

    return null
}

private fun getMethodPerNameAndParameterCount(klass: Class<*>, name: String, parameterCount: Int): Method? {
    val methods = klass.methods.filter { method ->
        method.isAccessible = true
        method.name == name && method.parameterCount == parameterCount
    }

    return when (methods.size) {
        0 -> null
        1 -> methods[0]
        else -> throw TooManyMethodsFoundWithParameterCountException(klass.name, name, parameterCount)
    }
}
