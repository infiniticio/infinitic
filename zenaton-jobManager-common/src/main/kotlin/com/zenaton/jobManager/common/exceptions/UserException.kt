package com.zenaton.jobManager.common.exceptions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.zenaton.common.data.SerializedData
import com.zenaton.common.json.Json
import com.zenaton.jobManager.common.Constants
import com.zenaton.jobManager.common.data.JobOptions

@JsonIgnoreProperties(ignoreUnknown = true)
sealed class UserException(
    open val msg: String,
    open val help: String
) : Exception("$msg\n$help")

open class UserExceptionInCommon(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

open class UserExceptionInClient(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

open class UserExceptionInWorker(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

/***********************
 * Exceptions in common
 ***********************/

class MissingMetaJavaClassDuringDeserialization(data: SerializedData) : UserExceptionInCommon(
    msg = "You can't deserialize SerializedData $data without explicitly providing return type if the data does not have one in its  \"${SerializedData.META_JAVA_CLASS}\" meta value",
    help = "Consider those options:\n" +
        "- update your data to include a \"${SerializedData.META_JAVA_CLASS}\" meta value describing the java class that should be used to deserialize your data\n" +
        "- update your code to use \"deserialize(klass: Class<*>)\" method"
)

class UnknownReturnClassDuringDeserialization(data: SerializedData, name: String) : UserExceptionInCommon(
    msg = "The return type \"$name\$ provided in the \"${SerializedData.META_JAVA_CLASS}\" meta value of $data is not a known java class",
    help = "Consider those options:\n" +
        "- if needed, fix the \"${SerializedData.META_JAVA_CLASS}\" meta value describing the java class that should be used to deserialize your data\n" +
        "- make sure the \"$name\" class is included in the code processed"
)

/***********************
 * Exceptions in client
 ***********************/

class NoMethodCallAtDispatch(name: String) : UserExceptionInClient(
    msg = "None method of $name has been provided to dispatch",
    help = "Make sure you call a method of the provided interface within the curly braces - example: infinitic.dispatch<FooInterface> { barMethod(*args) }"
)

class MultipleMethodCallsAtDispatch(name: String) : UserExceptionInClient(
    msg = "Only one method of $name can be dispatched at a time",
    help = "Make sure you call only one method of the provided interface within curly braces - multiple calls such as infinitic.dispatch<FooInterface> { barMethod(*args); bazMethod(*args) } is forbidden"
)

class ErrorDuringJsonSerializationOfParameter(parameterName: String, parameterValue: Any?, parameterType: String, methodName: String, className: String) : UserExceptionInClient(
    msg = "Error during Json serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

class ErrorDuringJsonDeserializationOfParameter(parameterName: String, parameterValue: Any?, parameterType: String, methodName: String, className: String) : UserExceptionInClient(
    msg = "Error during Json de-serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

class InconsistentJsonSerializationOfParameter(parameterName: String, parameterValue: Any?, restoredValue: Any?, parameterType: String, methodName: String, className: String) : UserExceptionInClient(
    msg = "Serialization/Deserialization of parameter $parameterName of $className::$methodName is not consistent: value provided $parameterValue - value after serialization/deserialization $restoredValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

/***********************
 * Exceptions in worker
 ***********************/

class InvalidUseOfDividerInJobName(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Job name \"$name\" must not contain the \"${Constants.METHOD_DIVIDER}\" divider",
    help = "You can not include \"${Constants.METHOD_DIVIDER}\" in your name's job, as it is also used as a divider between job's name and method"
)

class MultipleUseOfDividerInJobName(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Job name \"$name\" must not contain the \"${Constants.METHOD_DIVIDER}\" divider more than once",
    help = "You can not include \"${Constants.METHOD_DIVIDER}\" more than once in your name's job, as it is also used as a divider between job's name and method"
)

class ErrorDuringJobInstantiation(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Impossible to instantiate class \"$name\" using newInstance()",
    help = "Consider those options:\n" +
        "- adding an empty constructor to \"$name\"\n" +
        "- using \"register\" method to provide an instance that will be used associated to \"$name\""
)

class ClassNotFoundDuringJobInstantiation(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Impossible to find a Class associated to $name",
    help = "Consider those options:\n" +
        "- adding $name to the classes available to the worker\n" +
        "- using register method to provide an instance that will be used associated to $name"
)

class NoMethodFoundWithParameterTypes(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("method") val method: String,
    @JsonProperty("parameterTypes") val parameterTypes: List<String>
) : UserExceptionInWorker(
    msg = "No method \"$method(${ parameterTypes.joinToString() })\" found in \"$klass\" class",
    help = "Make sure parameter types are consistent with your method definition"
)

class NoMethodFoundWithParameterCount(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("method") val method: String,
    @JsonProperty("parameterCount") parameterCount: Int
) : UserExceptionInWorker(
    msg = "No method \"$method\" with $parameterCount parameters found in \"$klass\" class",
    help = ""
)

class TooManyMethodsFoundWithParameterCount(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("method") val method: String,
    @JsonProperty("parameterCount") parameterCount: Int
) : UserExceptionInWorker(
    msg = "Unable to decide which method \"$method\" with $parameterCount parameters to use in \"$klass\" job",
    help = ""
)

class DelayBeforeRetryReturnTypeError(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("actualType") val actualType: String,
    @JsonProperty("expectedType") val expectedType: String
) : UserExceptionInWorker(
    msg = "In \"$klass\" class, method ${Constants.DELAY_BEFORE_RETRY_METHOD} returns a $actualType, it must be a $expectedType",
    help = "Please update your method definition to return a $expectedType (or null)"
)

class ProcessingTimeout(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("delay") val delay: Float
) : UserExceptionInWorker(
    msg = "The processing of task \"$klass\" took more than $delay seconds",
    help = "You can increase (or remove entirely) this constraint in the options ${JobOptions::javaClass.name}"
)

class JobAttemptContextRetrievedOutsideOfProcessingThread() : UserExceptionInWorker(
    msg = "Worker.getContext() can be used only in the same thread that invoked the task.",
    help = "Check that your task do not try to retrieve its context from a new thread"
)

class JobAttemptContextSetFromExistingProcessingThread() : UserExceptionInWorker(
    msg = "A same thread can not process multiple tasks concurrently.",
    help = "Check that you do not use the same thread for multiple concurrent task processing"
)
