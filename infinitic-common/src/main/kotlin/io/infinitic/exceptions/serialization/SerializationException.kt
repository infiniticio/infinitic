/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.exceptions.serialization

import io.infinitic.exceptions.UserException

sealed class SerializationException(msg: String, help: String) : UserException("$msg.\n$help")

class SerializerNotFoundException(name: String) : SerializationException(
    msg = "Trying to deserialize data into '$name' but this class has no serializer",
    help =
    "Your data was correctly serialized, so make sure you use the same code base everywhere",
)

class KotlinDeserializationException(type: String, causeString: String) : SerializationException(
    msg =
    "Trying to deserialize data into '$type' but an error occurred during Kotlin deserialization: $causeString",
    help =
    "Make sure type '$type' can be safely serialized/deserialized using kotlinx.serialization",
)

class JsonDeserializationException(type: String, causeString: String) : SerializationException(
    msg =
    "Trying to deserialize data into '$type' but an error occurred during json deserialization: $causeString",
    help =
    "Make sure type '$type' can be safely serialized/deserialized in Json using FasterXML/jackson",
)

class ParameterSerializationException(
  parameterName: String,
  parameterType: String,
  methodName: String,
  className: String
) : SerializationException(
    msg =
    "Error during Json serialization of parameter '$parameterType $parameterName' of '$className.$methodName'",
    help = "",
)

class ParameterDeserializationException(
  parameterName: String,
  parameterType: String,
  methodName: String,
  className: String
) : SerializationException(
    msg =
    "Error during Json deserialization of parameter '$parameterType $parameterName' of '$className.$methodName'",
    help = "",
)

class ReturnValueSerializationException(
  methodName: String,
  className: String
) : SerializationException(
    msg =
    "Error during Json serialization of the return value of '$className.$methodName'",
    help = "",
)
