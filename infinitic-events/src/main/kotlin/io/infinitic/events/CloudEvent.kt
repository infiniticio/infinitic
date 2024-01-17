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

import com.github.avrokotlin.avro4k.Avro
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.v1.CloudEventV1
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.utils.getClass
import io.infinitic.currentVersion
import io.infinitic.events.Event.Companion.SCHEMAS_FOLDER
import io.infinitic.versions
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import org.apache.avro.Schema
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.reflect.KClass


private fun getEventType(classSimpleName: String) =
    classSimpleName.replaceFirstChar { it.lowercase() }

internal fun getSchemaURI(classSimpleName: String, version: String): URI =
    URI.create("${Event.SCHEMA_PREFIX}${getEventType(classSimpleName)}-$version${Event.SCHEMA_EXTENSION}")

internal fun toCloudEvent(
  eventId: String,
  partitionKey: String,
  classSimpleName: String,
  type: String,
  version: String,
  subject: String,
  timestamp: Instant,
  bytes: ByteArray
): CloudEvent = CloudEventBuilder.v1()
    .withId(eventId) // SHOULD BE MESSAGE ID
    .withSource(URI.create("infinitic.io"))
    // SHOULD BE urn:pulsar:cluster-name/tenant/namespace/topic-name/emitter-id
    // OR urn:pulsar:cluster-name/tenant/namespace/tasks/serviceName (/methodName ?)
    .withType(type) // infinitic.task.dispatched
    .withTime(OffsetDateTime.ofInstant(timestamp, ZoneOffset.UTC)) // publishedAt
    .withData(
        // DATA SHOULD BE DIFFERENT THAN KOTLIN DATA CLASSES
        "application/avro",
        getSchemaURI(classSimpleName, version),
        BytesCloudEventData.wrap(bytes),
    )
    .withSubject(subject) // TaskId
    //.withExtension("partitionkey", partitionKey) // TaskID?
    .build()

/**
 * Converts the CloudEvent to a Log object.
 *
 * @return the converted Log object as a Result. If the conversion is successful, the Result contains the Log object.
 *         If the conversion fails, the Result contains the corresponding error.
 */
internal fun CloudEvent.toInfiniticEvent(): Result<Event> = when (this) {
  is CloudEventV1 -> {
    val schema = getSchema().getOrElse { return Result.failure(it) }
    val klass: KClass<out Event> = getEventKClass().getOrElse { return Result.failure(it) }
    val serializer = @OptIn(InternalSerializationApi::class) klass.serializer()
    val bytes = getBytes().getOrElse { return Result.failure(it) }
    try {
      Result.success(AvroSerDe.readBinary(bytes, schema, serializer))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  else -> Result.failure(IllegalArgumentException("Unsupported CloudEvent version"))
}

private fun CloudEvent.getBytes() = data?.toBytes()?.let { Result.success(it) }
  ?: Result.failure(IllegalArgumentException("No data"))

/**
 * @return the class name associated with the CloudEvent's data.
 */
private fun CloudEvent.getClassName() =
    Event::class.java.`package`.name + "." + type.replaceFirstChar { it.uppercase() }

private fun CloudEvent.getEventKClass(): Result<KClass<out Event>> {
  val klass = getClassName().getClass().getOrElse { return Result.failure(it) }

  return when (Event::class.java.isAssignableFrom(klass)) {
    true -> @Suppress("UNCHECKED_CAST") Result.success((klass as Class<out Event>).kotlin)
    false -> Result.failure(IllegalArgumentException("$klass is not a Class<out Event>"))
  }
}

/**
 * Retrieves the schema associated with the CloudEvent.
 *
 * @return a Result object containing the schema if retrieval is successful, or an error if retrieval fails.
 */
private fun CloudEvent.getSchema(): Result<Schema> {
  fun error(msg: String, e: Throwable? = null): Result<Schema> =
      Result.failure(IllegalArgumentException("Unsupported CloudEvent - $msg", e))

  val schemaUri = this.dataSchema?.toASCIIString() ?: return error("Missing data schema")

  // uri should start with SCHEMA_PREFIX
  if (!schemaUri.startsWith(Event.SCHEMA_PREFIX)) return error("Unknown data schema '$schemaUri'")
  val file = schemaUri.replaceFirst(Event.SCHEMA_PREFIX, "")

  // uri should end with SCHEMA_EXTENSION
  if (!file.endsWith(Event.SCHEMA_EXTENSION)) return error("Invalid data schema file extension in '$file'")
  val prefix = file.substringBeforeLast(".")

  // prefix should be something like "taskStarted-0.12.2.avsc" or "taskStarted-0.12.2-SNAPSHOT.avsc"
  val c = prefix.split("-", limit = 2)
  if (c.size != 2) return error("Invalid data schema, $prefix should be a combination of class and version")

  if (c[0] != type) return error("Inconsistent event type '$type' and data schema '${c[0]}")
  val eventClassName = getClassName()

  when (val eventVersion = c[1]) {
    // if current version, we return the schema directly from the class
    currentVersion -> {
      val klass = eventClassName.getClass().getOrElse {
        return error("Unknown class $eventClassName", it)
      }.kotlin

      @OptIn(InternalSerializationApi::class)
      val serializer = klass.serializerOrNull() ?: return error("No serializer for class $klass")

      return Result.success(Avro.default.schema(serializer))
    }
    // if released version, we return the schema from the resources
    in versions -> {
      val schemaFile = Event::class.java.getResource("/$SCHEMAS_FOLDER/$file")
        ?: return error("Unknown schema file $file")

      return Result.success(Schema.Parser().parse(schemaFile.readText()))
    }

    else -> return error("Unknown version $eventVersion of $eventClassName")
  }
}
