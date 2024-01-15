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
package io.infinitic.common.workflows.engine.state

import io.infinitic.common.fixtures.checkBackwardCompatibility
import io.infinitic.common.fixtures.checkOrCreateCurrentFile
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.avro.AvroSerDe.getAllSchemas
import io.infinitic.common.serDe.avro.AvroSerDe.getRandomBinaryWithSchemaFingerprint
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class WorkflowStateTests :
  StringSpec(
      {
        "WorkflowState should be avro-convertible" {
          shouldNotThrowAny {
            val msg = TestFactory.random<WorkflowState>()
            val bytes: ByteArray = msg.toByteArray()

            WorkflowState.fromByteArray(bytes) shouldBe msg
          }
        }

        "Avro schema should be backward compatible to 0.9.0" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(WorkflowState::class, WorkflowState.serializer())

          checkBackwardCompatibility(WorkflowState::class, WorkflowState.serializer())
        }

        "We should be able to read binary from any previous version since 0.9.0" {
          getAllSchemas(WorkflowState::class).forEach { (_, schema) ->
            val bytes = getRandomBinaryWithSchemaFingerprint(schema)

            shouldNotThrowAny { WorkflowState.fromByteArray(bytes) }
          }
        }
      },
  )
