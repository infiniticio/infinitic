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
package io.infinitic.common.workflows.data.workflowTasks

import io.infinitic.common.fixtures.checkBackwardCompatibility
import io.infinitic.common.fixtures.checkOrCreateCurrentFile
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.serDe.SerializedDataType
import io.infinitic.common.serDe.avro.AvroSerDe
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class WorkflowTaskParametersTests :
  StringSpec(
      {
        "WorkflowTaskParameters should be SerializedData-convertible" {
          shouldNotThrowAny {
            val msg = TestFactory.random<WorkflowTaskParameters>()
            val msg2 = SerializedData.from(msg).deserialize()

            msg shouldBe msg2
          }
        }

        "WorkflowTaskParameters SerializedData should use finger printed schema" {
          val msg = TestFactory.random<WorkflowTaskParameters>()
          val data = SerializedData.from(msg)
          data.type shouldBe SerializedDataType.AVRO_WITH_SCHEMA
          data.bytes.contentEquals(msg.toByteArray()) shouldBe true
        }

        "We should be able to read WorkflowTaskParameters from any previous version since 0.9.0" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(
              WorkflowTaskParameters::class,
              WorkflowTaskParameters.serializer(),
          )

          checkBackwardCompatibility(
              WorkflowTaskParameters::class,
              WorkflowTaskParameters.serializer(),
          )
        }

        "We should be able to read binary from any previous version since 0.9.0" {
          AvroSerDe.getAllSchemas(WorkflowTaskParameters::class).forEach { (_, schema) ->
            val bytes = AvroSerDe.getRandomBinaryWithSchemaFingerprint(schema)

            shouldNotThrowAny { WorkflowTaskParameters.fromByteArray(bytes) }
          }
        }
      },
  )
