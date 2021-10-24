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

package io.infinitic.exceptions.serialization

import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.serDe.SerializedDataType
import io.infinitic.common.serDe.json.Json
import io.infinitic.exceptions.UserException

sealed class SerializationException(
    msg: String,
    help: String
) : UserException("$msg.\n$help")

object MissingMetaJavaClassException : SerializationException(
    msg = "Trying to deserialize data without explicitly providing java class in \"${SerializedData.META_JAVA_CLASS}\" meta value",
    help = "You are probably trying to deserialize data generated by non-Java code. " +
        "Update this non-java code to include a \"${SerializedData.META_JAVA_CLASS}\" meta value " +
        "describing the java class that should be used"
)

class ClassNotFoundException(
    name: String
) : SerializationException(
    msg = "Trying to deserialize data into \"$name\" but this class is unknown",
    help = "Please make sure to include this class in your code base."
)

class WrongSerializationTypeException(
    name: String?,
    type: SerializedDataType
) : SerializationException(
    msg = "Trying to retrieve json from \"$name\" serialized data, but the serialized format is $type",
    help = ""
)

class SerializerNotFoundException(
    name: String
) : SerializationException(
    msg = "Trying to deserialize data into \"$name\" but this class has no serializer",
    help = "Your data was correctly serialized, so make sure you use the same code base everywhere"
)

class KotlinDeserializationException(
    name: String,
    causeString: String
) : SerializationException(
    msg = "Trying to deserialize data into \"$name\" but an error occurred during Kotlin deserialization: $causeString",
    help = "Please make sure your class \"$name\" can be safely serialized/deserialized using kotlinx.serialization and avro4k"
)

class JsonDeserializationException(
    name: String,
    causeString: String
) : SerializationException(
    msg = "Trying to deserialize data into \"$name\" but an error occurred during json deserialization: $causeString",
    help = "Please make sure your class \"$name\" can be safely serialized/deserialized in Json using FasterXML/jackson"
)

class ParameterSerializationException(
    parameterName: String,
    parameterType: String,
    methodName: String,
    className: String
) : SerializationException(
    msg = "Error during Json serialization of parameter $parameterName of $className::$methodName",
    help = "We are using Jackson library per default to serialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType by more simple parameters in $className::$methodName\n"
)
