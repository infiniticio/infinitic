package io.infinitic.taskManager.common.parser

import io.infinitic.taskManager.common.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.taskManager.common.exceptions.ErrorDuringInstantiation
import io.infinitic.taskManager.common.exceptions.NoMethodFoundWithParameterCount
import io.infinitic.taskManager.common.exceptions.NoMethodFoundWithParameterTypes
import io.infinitic.taskManager.common.exceptions.TooManyMethodsFoundWithParameterCount
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
