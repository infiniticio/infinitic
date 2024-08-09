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

sealed class SerializationException(msg: String, cause: Throwable? = null) :
  UserException("$msg.", cause)

@Deprecated("this should not be used anymore after 0.15.0")
class SerializerNotFoundException(name: String) : SerializationException(
    "Trying to deserialize data into '$name' but this class has no serializer",
)

class KotlinSerializationException(obj: Any, cause: Throwable) :
  SerializationException(
      "Unable to serialize the object '$obj' (class '${obj::class.java.name}') using kotlinx.serialization",
      cause,
  )

class JsonSerializationException(obj: Any, type: String?, cause: Throwable) :
  SerializationException(
      "Unable to serialize " + (type?.let { "with type '$type' " } ?: "") +
          "the object '$obj' (class '${obj::class.java.name}') using FasterXML/jackson",
      cause,
  )

class KotlinDeserializationException(json: String, type: String?, cause: Throwable) :
  SerializationException(
      "Unable to deserialize '$json' "
          + (type?.let { "into type '$type' " } ?: "") + "using kotlinx.serialization",
      cause,
  )

class JsonDeserializationException(json: String, type: String?, cause: Throwable) :
  SerializationException(
      "Unable to deserialize '$json' "
          + (type?.let { "into type '$type' " } ?: "") + "using FasterXML/jackson",
      cause,
  )

class ArgumentSerializationException(
  className: String,
  methodName: String,
  parameterName: String,
  parameterType: String,
  cause: Throwable
) : SerializationException(
    "Error during Json serialization of parameter '$parameterName' (type '$parameterType') of method '$methodName' (class '$className')",
    cause,
)

class ArgumentDeserializationException(
  className: String,
  methodName: String,
  parameterName: String,
  parameterType: String,
  cause: Throwable
) : SerializationException(
    "Error during Json deserialization of parameter '$parameterName' (type '$parameterType') of method '$methodName' (class '$className')",
    cause,
)

class ReturnValueSerializationException(
  className: String,
  methodName: String,
  cause: Throwable
) : SerializationException(
    "Error during Json serialization of the return value of method '$methodName' (class '$className')",
    cause,
)

class ReturnValueDeserializationException(
  className: String,
  methodName: String,
  cause: Throwable
) : SerializationException(
    "Error during deserialization of the return value of method '$methodName' (class '$className')",
    cause,
)

