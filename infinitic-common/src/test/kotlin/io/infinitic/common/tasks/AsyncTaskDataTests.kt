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
import io.infinitic.common.tasks.data.AsyncTaskData
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AsyncTaskDataTests :
  StringSpec(
      {
        "AsyncTaskData should be avro-convertible" {
          shouldNotThrowAny {
            val data = TestFactory.random<AsyncTaskData>()
            val bytes: ByteArray = data.toByteArray()

            AsyncTaskData.fromByteArray(bytes) shouldBe data
          }
        }

        "AsyncTaskData Avro schema should be backward compatible" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(AsyncTaskData::class, AsyncTaskData.serializer())

          checkBackwardCompatibility(AsyncTaskData::class, AsyncTaskData.serializer())
        }

        "We should be able to read AsyncTaskData binary from any previous version" {
          AvroSerDe.getAllSchemas(AsyncTaskData::class).forEach { (_, schema) ->
            val bytes = AvroSerDe.getRandomBinaryWithSchemaFingerprint(schema)

            shouldNotThrowAny { AsyncTaskData.fromByteArray(bytes) }
          }
        }
      },
  )
