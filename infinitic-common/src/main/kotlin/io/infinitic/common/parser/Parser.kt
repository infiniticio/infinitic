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
import io.infinitic.exceptions.tasks.ClassNotFoundException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterCountException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterCountException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterTypesException
import java.lang.reflect.Method

fun getClassForName(name: String): Class<out Any> = when (name) {
    "bytes" -> Byte::class.java
    "short" -> Short::class.java
    "int" -> Int::class.java
    "long" -> Long::class.java
    "float" -> Float::class.java
    "double" -> Double::class.java
    "boolean" -> Boolean::class.java
    "char" -> Character::class.java
    else ->
        try {
            Class.forName(name)
        } catch (e: java.lang.ClassNotFoundException) {
            throw ClassNotFoundException(name)
        }
}

// TODO: support methods with varargs
fun getMethodPerNameAndParameters(obj: Any, name: String, parameterTypes: List<String>?, parametersCount: Int): Method {
    val className by lazy { obj::class.java.name }
    val methods: Array<Method> = obj::class.javaObjectType.methods
    return when (parameterTypes) {
        null -> getMethodPerAnnotationAndParameterCount(className, methods, name, parametersCount)
            ?: getMethodPerNameAndParameterCount(className, methods, name, parametersCount)
            ?: throw NoMethodFoundWithParameterCountException(className, name, parametersCount)
        else -> getMethodPerAnnotationAndParameterTypes(className, methods, name, parameterTypes)
            ?: getMethodPerNameAndParameterTypes(className, methods, name, parameterTypes)
            ?: throw NoMethodFoundWithParameterTypesException(className, name, parameterTypes)
    }
}

private fun getMethodPerAnnotationAndParameterTypes(className: String, methods: Array<Method>, name: String, parameterTypes: List<String>): Method? {
    val candidates = methods.filter { it.getAnnotation(Name::class.java)?.name == name && it.parameterTypes.map { it.name } == parameterTypes }
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw TooManyMethodsFoundWithParameterTypesException(className, name, parameterTypes)
    }
}

private fun getMethodPerNameAndParameterTypes(className: String, methods: Array<Method>, name: String, parameterTypes: List<String>): Method? {
    val candidates = methods.filter { it.name == name && it.parameterTypes.map { it.name } == parameterTypes }
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw TooManyMethodsFoundWithParameterTypesException(className, name, parameterTypes)
    }
}

private fun getMethodPerAnnotationAndParameterCount(className: String, methods: Array<Method>, name: String, parameterCount: Int): Method? {
    val candidates = methods.filter { it.getAnnotation(Name::class.java)?.name == name && it.parameterCount == parameterCount }
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw TooManyMethodsFoundWithParameterCountException(className, name, parameterCount)
    }
}

private fun getMethodPerNameAndParameterCount(className: String, methods: Array<Method>, name: String, parameterCount: Int): Method? {
    val candidates = methods.filter { it.name == name && it.parameterCount == parameterCount }
    return when (candidates.size) {
        0 -> null
        1 -> candidates[0]
        else -> throw TooManyMethodsFoundWithParameterCountException(className, name, parameterCount)
    }
}
