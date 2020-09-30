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

package io.infinitic.common.tasks.parser

import io.infinitic.common.tasks.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.common.tasks.exceptions.ErrorDuringInstantiation
import io.infinitic.common.tasks.exceptions.NoMethodFoundWithParameterCount
import io.infinitic.common.tasks.exceptions.NoMethodFoundWithParameterTypes
import io.infinitic.common.tasks.exceptions.TooManyMethodsFoundWithParameterCount
import java.lang.reflect.Method

public fun getNewInstancePerName(name: String): Any {
    val klass = getClassForName(name)
    return try {
        klass.newInstance()
    } catch (e: Exception) {
        println(e.cause)
        throw ErrorDuringInstantiation(name)
    }
}

public fun getClassForName(name: String): Class<out Any> = when (name) {
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
        } catch (e: ClassNotFoundException) {
            throw ClassNotFoundDuringInstantiation(name)
        }
}

// TODO: currently methods with "suspend" keyword are not supported
// TODO: currently methods with varargs parameters are not supported
public fun getMethodPerNameAndParameterTypes(obj: Any, methodName: String, parameterTypes: List<String>): Method {
    val parameterClasses = parameterTypes.map { getClassForName(it) }.toTypedArray()
    try {
        return obj::class.java.getMethod(methodName, *parameterClasses)
    } catch (e: NoSuchMethodException) {
        throw NoMethodFoundWithParameterTypes(obj::class.java.name, methodName, parameterClasses.map { it.name })
    }
}

// TODO: currently methods with "suspend" keyword are not supported
// TODO: currently methods with varargs parameters are not supported
public fun getMethodPerNameAndParameterCount(obj: Any, methodName: String, parameterCount: Int): Method {
    val methods = obj::class.javaObjectType.methods.filter { it.name == methodName && it.parameterCount == parameterCount }
    if (methods.isEmpty()) throw NoMethodFoundWithParameterCount(obj::class.java.name, methodName, parameterCount)
    if (methods.size > 1) throw TooManyMethodsFoundWithParameterCount(obj::class.java.name, methodName, parameterCount)

    return methods[0]
}
