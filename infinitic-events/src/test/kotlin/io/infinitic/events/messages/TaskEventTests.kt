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
package io.infinitic.events.messages

import io.cloudevents.CloudEvent
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.checkBackwardCompatibility
import io.infinitic.common.fixtures.checkOrCreateCurrentFile
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.events.Event
import io.infinitic.events.TaskEvent
import io.infinitic.events.toByteArray
import io.infinitic.events.toCloudEvent
import io.infinitic.events.toLog
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class)
class TaskEventTests : StringSpec(
    {
      TaskEvent::class.sealedSubclasses.forEach { klass ->
        "${klass.simpleName} should be Avro-convertible" {
          val msg = TestFactory.random(klass)
          val bytes = msg.toByteArray()
          val decodedMsg = fromByteArray(bytes, klass)

          decodedMsg shouldBe msg
        }
      }

      TaskEvent::class.sealedSubclasses.forEach { klass ->
        "${klass.simpleName} should be CloudEvents-convertible" {
          val msg = TestFactory.random(klass)
          val ce = msg.toCloudEvent()
          ce.shouldBeInstanceOf<CloudEvent>()

          val decodedCe = shouldNotThrowAny { ce.toLog().getOrThrow() }

          decodedCe shouldBe msg
        }
      }

      TaskEvent::class.sealedSubclasses.forEach { klass ->
        AvroSerDe.getAllSchemas(klass).forEach { (version, schema) ->
          "We should be able to decode ${klass.simpleName} from binary version $version" {
            println(schema)
            val bytes = AvroSerDe.getRandomBinary(schema)

            shouldNotThrowAny { fromByteArray(bytes, schema, klass) }
          }
        }
      }

      TaskEvent::class.sealedSubclasses.forEach { klass ->
        AvroSerDe.getAllSchemas(klass).forEach { (version, schema) ->
          "We should be able to decode ${klass.simpleName} from CloudEvent version $version" {
            // create random data from schema
            val bytes = AvroSerDe.getRandomBinary(schema)
            // get random data, we know this works thx to tests above
            val msg = fromByteArray(bytes, schema, klass) as TaskEvent
            // hydrate CloudEvent
            val ce = toCloudEvent(
                eventId = msg.ceEventId,
                classSimpleName = klass.java.simpleName,
                type = msg.ceType,
                version = version,
                timestamp = msg.timestamp,
                bytes = bytes,
            )
            // test that we can convert this CloudEvent to a Log object
            shouldNotThrowAny { ce.toLog() }
          }
        }
      }

      fun <T : TaskEvent> StringSpec.checkBackwardCompatibility(klass: KClass<T>) {
        "Avro Schema of ${klass.simpleName} should be backward compatible from all versions" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(klass, klass.serializer())

          checkBackwardCompatibility(klass, klass.serializer())
        }
      }

      TaskEvent::class.sealedSubclasses.forEach { klass ->
        checkBackwardCompatibility(klass)
      }
    },
)

private fun fromByteArray(bytes: ByteArray, klass: KClass<out Event>): Event {
  @OptIn(InternalSerializationApi::class) val serializer = klass.serializer()
  val currentSchema = AvroSerDe.currentSchema(serializer)
  return AvroSerDe.readBinary(bytes, currentSchema, serializer)
}

private fun fromByteArray(bytes: ByteArray, readerSchema: Schema, klass: KClass<out Event>): Event {
  @OptIn(InternalSerializationApi::class) val serializer = klass.serializer()
  return AvroSerDe.readBinary(bytes, readerSchema, serializer)
}
