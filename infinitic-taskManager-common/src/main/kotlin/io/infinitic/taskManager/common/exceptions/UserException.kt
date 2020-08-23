package io.infinitic.taskManager.common.exceptions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.infinitic.common.data.SerializedData
import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.Constants
import io.infinitic.taskManager.common.data.TaskOptions

/*
 *  @JsonIgnoreProperties and @JsonProperty annotations are here
 *  to allow correct JSON ser/deserialization through constructors
 */

@JsonIgnoreProperties(ignoreUnknown = true)
sealed class UserException(
    open val msg: String,
    open val help: String
) : Exception("$msg.\n$help")

sealed class UserExceptionInCommon(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

sealed class UserExceptionInClient(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

sealed class UserExceptionInWorker(
    override val msg: String,
    override val help: String
) : UserException(msg, help)

/***********************
 * Exceptions in common
 ***********************/

data class MissingMetaJavaClassDuringDeserialization(
    @JsonProperty("data") val data: SerializedData
) : UserExceptionInCommon(
    msg = "You can't deserialize SerializedData $data without explicitly providing return type if the data does not have one in its  \"${SerializedData.META_JAVA_CLASS}\" meta value",
    help = "Consider those options:\n" +
        "- update your data to include a \"${SerializedData.META_JAVA_CLASS}\" meta value describing the java class that should be used to deserialize your data\n" +
        "- update your code to use \"deserialize(klass: Class<*>)\" method"
)

data class UnknownReturnClassDuringDeserialization(
    @JsonProperty("data") val data: SerializedData,
    @JsonProperty("name") val name: String
) : UserExceptionInCommon(
    msg = "The return type \"$name\$ provided in the \"${SerializedData.META_JAVA_CLASS}\" meta value of $data is not a known java class",
    help = "Consider those options:\n" +
        "- if needed, fix the \"${SerializedData.META_JAVA_CLASS}\" meta value describing the java class that should be used to deserialize your data\n" +
        "- make sure the \"$name\" class is included in the code processed"
)

/***********************
 * Exceptions in client
 ***********************/

data class NoMethodCallAtDispatch(
    @JsonProperty("name") val name: String,
    @JsonProperty("dispatch") val dispatch: String

) : UserExceptionInClient(
    msg = "You must use a method of \"$name\" when using $dispatch method",
    help = "Make sure to call one method of \"$name\" within the curly braces - example: client.$dispatch<Foo> { bar(*args) }"
)

data class MultipleMethodCallsAtDispatch(
    @JsonProperty("name") val name: String
) : UserExceptionInClient(
    msg = "Only one method of $name can be dispatched at a time",
    help = "Make sure you call only one method of the provided interface within curly braces - multiple calls such as infinitic.dispatch<FooInterface> { barMethod(*args); bazMethod(*args) } is forbidden"
)

data class ErrorDuringJsonSerializationOfParameter(
    @JsonProperty("parameterName") val parameterName: String,
    @JsonProperty("parameterValue") val parameterValue: Any?,
    @JsonProperty("parameterType") val parameterType: String,
    @JsonProperty("methodName") val methodName: String,
    @JsonProperty("className") val className: String
) : UserExceptionInClient(
    msg = "Error during Json serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

data class ErrorDuringJsonDeserializationOfParameter(
    @JsonProperty("parameterName") val parameterName: String,
    @JsonProperty("parameterValue") val parameterValue: Any?,
    @JsonProperty("parameterType") val parameterType: String,
    @JsonProperty("methodName") val methodName: String,
    @JsonProperty("className") val className: String
) : UserExceptionInClient(
    msg = "Error during Json de-serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

data class InconsistentJsonSerializationOfParameter(
    @JsonProperty("parameterName") val parameterName: String,
    @JsonProperty("parameterValue") val parameterValue: Any?,
    @JsonProperty("restoredValue") val restoredValue: Any?,
    @JsonProperty("parameterType") val parameterType: String,
    @JsonProperty("methodName") val methodName: String,
    @JsonProperty("className") val className: String
) : UserExceptionInClient(
    msg = "Serialization/Deserialization of parameter $parameterName of $className::$methodName is not consistent: value provided $parameterValue - value after serialization/deserialization $restoredValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

/***********************
 * Exceptions in worker
 ***********************/

data class InvalidUseOfDividerInTaskName(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Task name \"$name\" must not contain the \"${Constants.METHOD_DIVIDER}\" divider",
    help = "You can not include \"${Constants.METHOD_DIVIDER}\" in your name's job, as it is also used as a divider between job's name and method"
)

data class MultipleUseOfDividerInTaskName(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Task name \"$name\" must not contain the \"${Constants.METHOD_DIVIDER}\" divider more than once",
    help = "You can not include \"${Constants.METHOD_DIVIDER}\" more than once in your name's job, as it is also used as a divider between job's name and method"
)

data class ErrorDuringTaskInstantiation(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Impossible to instantiate class \"$name\" using newInstance()",
    help = "Consider those options:\n" +
        "- adding an empty constructor to \"$name\"\n" +
        "- using \"register\" method to provide an instance that will be used associated to \"$name\""
)

data class ClassNotFoundDuringTaskInstantiation(
    @JsonProperty("name") val name: String
) : UserExceptionInWorker(
    msg = "Impossible to find a Class associated to $name",
    help = "Consider those options:\n" +
        "- adding $name to the classes available to the worker\n" +
        "- using register method to provide an instance that will be used associated to $name"
)

data class NoMethodFoundWithParameterTypes(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("method") val method: String,
    @JsonProperty("parameterTypes") val parameterTypes: List<String>
) : UserExceptionInWorker(
    msg = "No method \"$method(${ parameterTypes.joinToString() })\" found in \"$klass\" class",
    help = "Make sure parameter types are consistent with your method definition"
)

data class NoMethodFoundWithParameterCount(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("method") val method: String,
    @JsonProperty("parameterCount") val parameterCount: Int
) : UserExceptionInWorker(
    msg = "No method \"$method\" with $parameterCount parameters found in \"$klass\" class",
    help = ""
)

data class TooManyMethodsFoundWithParameterCount(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("method") val method: String,
    @JsonProperty("parameterCount") val parameterCount: Int
) : UserExceptionInWorker(
    msg = "Unable to decide which method \"$method\" with $parameterCount parameters to use in \"$klass\" job",
    help = ""
)

data class RetryDelayHasWrongReturnType(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("actualType") val actualType: String,
    @JsonProperty("expectedType") val expectedType: String
) : UserExceptionInWorker(
    msg = "In \"$klass\" class, method ${Constants.DELAY_BEFORE_RETRY_METHOD} returns a $actualType, it must be a $expectedType",
    help = "Please update your method definition to return a $expectedType (or null)"
)

data class ProcessingTimeout(
    @JsonProperty("klass") val klass: String,
    @JsonProperty("delay") val delay: Float
) : UserExceptionInWorker(
    msg = "The processing of task \"$klass\" took more than $delay seconds",
    help = "You can increase (or remove entirely) this constraint in the options ${TaskOptions::javaClass.name}"
)

data class TaskAttemptContextRetrievedOutsideOfProcessingThread(val unused: String? = null) : UserExceptionInWorker(
    msg = "Worker.getContext() can be used only in the same thread that invoked the task",
    help = "Check that your task do not try to retrieve its context from a new thread"
)

data class TaskAttemptContextSetFromExistingProcessingThread(val unused: String? = null) : UserExceptionInWorker(
    msg = "A same thread can not process multiple tasks concurrently",
    help = "Check that you do not use the same thread for multiple concurrent task processing"
)

data class ExceptionDuringParametersDeserialization(
    @JsonProperty("taskName") val taskName: String,
    @JsonProperty("methodName") val methodName: String,
    @JsonProperty("input") val input: List<SerializedData>,
    @JsonProperty("parameterTypes") val parameterTypes: List<String>
) : UserExceptionInWorker(
    msg = "Impossible to deserialize input \"$input\" with types \"$parameterTypes\" for method \"$taskName::$methodName\"",
    help = ""
)

data class CanNotUseJavaParameterTypesInMeta(
    @JsonProperty("reserved") val reserved: String
) : UserExceptionInWorker(
    msg = "\"$reserved\" is as reserved keyword, you can not use it in your meta data",
    help = ""
)
