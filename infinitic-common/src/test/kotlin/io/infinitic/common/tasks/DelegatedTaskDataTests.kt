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
package io.infinitic.common.tasks

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.checkBackwardCompatibility
import io.infinitic.common.fixtures.checkOrCreateCurrentFile
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.DelegatedTaskData
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DelegatedTaskDataTests :
  StringSpec(
      {
        "DelegatedTaskData should be avro-convertible" {
          shouldNotThrowAny {
            val data = TestFactory.random<DelegatedTaskData>()
            val bytes: ByteArray = data.toByteArray()

            DelegatedTaskData.fromByteArray(bytes) shouldBe data
          }
        }

        "DelegatedTaskData Avro schema should be backward compatible" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(DelegatedTaskData::class, DelegatedTaskData.serializer())

          checkBackwardCompatibility(DelegatedTaskData::class, DelegatedTaskData.serializer())
        }

        "We should be able to read DelegatedTaskData binary from any previous version" {
          AvroSerDe.getAllSchemas(DelegatedTaskData::class).forEach { (_, schema) ->
            val bytes = AvroSerDe.getRandomBinaryWithSchemaFingerprint(schema)

            shouldNotThrowAny { DelegatedTaskData.fromByteArray(bytes) }
          }
        }
      },
  )
