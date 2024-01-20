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

package io.infinitic.events

import io.cloudevents.CloudEvent
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.events.data.RequesterData
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.time.Instant


/**
 * Represents a log entry .
 *
 * @property timestamp is the timestamp of the underlying message
 * @property requester is needed for traceability
 * @property emitterName is the name of the emitter of this log
 * @property ceEventId is the id of the CloudEvent version of this log
 * @property ceType is the type of the CloudEvent version of this log
 */
@Serializable
sealed interface Event {
  val timestamp: Instant
  val requester: RequesterData
  val emitterName: EmitterName

  val ceEventId: String
  val ceType: String

  companion object {

    internal const val SCHEMAS_FOLDER = "schemas"

    internal const val SCHEMA_PREFIX = "https://raw.githubusercontent.com/" +
        "infiniticio/infinitic/main/infinitic-events/src/main/resources/$SCHEMAS_FOLDER/"

    internal const val SCHEMA_EXTENSION = ".avsc"

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun from(ce: CloudEvent): Event = ce.toInfiniticEvent().getOrThrow()
  }
}

/**
 * Converts a Log object to a CloudEvent representation.
 *
 * @return The CloudEvent object.
 */
//internal fun Event.toCloudEvent(): CloudEvent = toCloudEvent(
//    eventId = ceEventId,
//    partitionKey =,//,
//    classSimpleName = this::class.java.simpleName,
//    version = currentVersion,
//    type = ceType,
//    subject = ceSubject,
//    timestamp = timestamp,
//    bytes = toByteArray(),
//)

/**
 * Converts the Log object to a byte array using Avro serialization.
 *
 * we use AvroSerDe.writeBinary instead of AvroSerDe.writeBinaryWithSchemaFingerprint
 * because the schema reference is embedded into the CloudEvents
 * (and we must do that to be compliant with CloudEvents)
 *
 * @return the byte array representation of the Log object
 */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
fun <T : Event> T.toByteArray() =
    AvroSerDe.writeBinary(this, this::class.serializer() as KSerializer<T>)




