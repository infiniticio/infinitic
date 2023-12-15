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
package io.infinitic.logs.messages

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.avro.AvroSerDe
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import org.apache.avro.Schema

class TaskLogTests :
  StringSpec(
      {
        val isAvroConvertible = { klass: KClass<out TaskLog> ->
          "${klass.simpleName} should be avro-convertible" {
            val msg = TestFactory.random(klass)
            val bytes = msg.toByteArray()
            val decodedMsg = fromByteArray(msg, bytes)

            decodedMsg shouldBe msg
          }
        }

        TaskCommandLog::class.sealedSubclasses.forEach(isAvroConvertible)
        TaskEventLog::class.sealedSubclasses.forEach(isAvroConvertible)

        val canReadPreviousSchema = { klass: KClass<out TaskLog>, version: String, schema: Schema ->
          "We should be able to decode ${klass.simpleName} from binary version $version" {
            val bytes = AvroSerDe.getRandomBinaryWithSchemaFingerprint(schema)

            shouldNotThrowAny { fromByteArray(TestFactory.random(klass), bytes) }
          }
        }

        TaskCommandLog::class.sealedSubclasses.forEach { klass ->
          AvroSerDe.getAllSchemas(klass).forEach { (version, schema) ->
            canReadPreviousSchema(klass, version, schema)
          }
        }

        TaskEventLog::class.sealedSubclasses.forEach { klass ->
          AvroSerDe.getAllSchemas(klass).forEach { (version, schema) ->
            canReadPreviousSchema(klass, version, schema)
          }
        }
      }
  )

private fun fromByteArray(log: TaskLog, bytes: ByteArray) = when(log) {
  is RunTask -> RunTask.fromByteArray(bytes)
  is TaskCompleted -> TaskCompleted.fromByteArray(bytes)
  is TaskFailed -> TaskFailed.fromByteArray(bytes)
}
