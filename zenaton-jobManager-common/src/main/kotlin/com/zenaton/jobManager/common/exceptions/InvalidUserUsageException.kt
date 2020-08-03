package com.zenaton.jobManager.common.exceptions

import com.zenaton.common.data.SerializedData
import com.zenaton.common.json.Json

sealed class InvalidUserUsageException(
    open val msg: String,
    open val help: String
) : Exception("$msg\n$help")

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

class ErrorDuringJsonDeserializationOfParameter(parameterName: String, parameterValue: Any?, parameterType: String, methodName: String, className: String) : InvalidUserUsageException(
    msg = "Error during Json de-serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

class InconsistentJsonSerializationOfParameter(parameterName: String, parameterValue: Any?, restoredValue: Any?, parameterType: String, methodName: String, className: String) : InvalidUserUsageException(
    msg = "Serialization/Deserialization of parameter $parameterName of $className::$methodName is not consistent: value provided $parameterValue - value after serialization/deserialization $restoredValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

class MissingMetaJavaClassDuringDeserialization(data: SerializedData) : InvalidUserUsageException(
    msg = "You can't deserialize SerializedData $data without explicitly providing return type if the data does not have one in its  \"${SerializedData.META_JAVA_CLASS}\" meta value",
    help = "Consider those options:\n" +
        "- update your data to include a \"${SerializedData.META_JAVA_CLASS}\" meta value describing the java class that should be used to deserialize your data\n" +
        "- update your code to use \"deserialize(klass: Class<*>)\" method"
)

class UnknownReturnClassDuringDeserialization(data: SerializedData, name: String) : InvalidUserUsageException(
    msg = "The return type \"$name\$ provided in the \"${SerializedData.META_JAVA_CLASS}\" meta value of $data is not a known java class",
    help = "Consider those options:\n" +
        "- if needed, fix the \"${SerializedData.META_JAVA_CLASS}\" meta value describing the java class that should be used to deserialize your data\n" +
        "- make sure the \"$name\" class is included in the code processed"
)
