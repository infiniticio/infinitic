package com.zenaton.jobManager.common.exceptions

import com.zenaton.common.json.Json

sealed class InvalidUserUsageException(
    open val msg: String,
    open val help: String
) : Exception(msg)

class NoMethodCallAtDispatch(name: String) : InvalidUserUsageException(
    msg = "None method of $name has been provided to dispatch",
    help = "Make sure you call a method of the provided interface within the curly braces - example: infinitic.dispatch<FooInterface> { barMethod(*args) }"
)

class MultipleMethodCallsAtDispatch(name: String) : InvalidUserUsageException(
    msg = "Only one method of $name can be dispatched at a time",
    help = "Make sure you call only one method of the provided interface within curly braces - multiple calls such as infinitic.dispatch<FooInterface> { barMethod(*args); bazMethod(*args) } is forbidden"
)

class ErrorDuringJsonSerializationOfParameter(parameterName: String, parameterValue: Any?, parameterType: String, methodName: String, className: String) : InvalidUserUsageException(
    msg = "Error during Json serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)
