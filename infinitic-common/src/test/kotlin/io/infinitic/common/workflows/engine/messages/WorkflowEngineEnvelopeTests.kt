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
package io.infinitic.common.workflows.engine.messages

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.checkBackwardCompatibility
import io.infinitic.common.fixtures.checkOrCreateCurrentFile
import io.infinitic.common.serDe.avro.AvroSerDe
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe

class WorkflowEngineEnvelopeTests :
  StringSpec(
      {
        WorkflowStateEngineMessage::class.sealedSubclasses.map {
          val msg = TestFactory.random(it)

          "WorkflowEngineMessage: ${msg::class.simpleName} should be have its workflowId as key" {
            msg.key() shouldBe msg.workflowId.toString()
          }
        }

        WorkflowStateEngineMessage::class.sealedSubclasses.map {
          val msg = TestFactory.random(it)

          "WorkflowEngineEnvelope: ${msg::class.simpleName} should be avro-convertible" {
            shouldNotThrowAny {
              val envelope = WorkflowEngineEnvelope.from(msg)
              val byteArray = envelope.toByteArray()

              WorkflowEngineEnvelope.fromByteArray(
                  byteArray,
                  WorkflowEngineEnvelope.writerSchema,
              ) shouldBe envelope
            }
          }
        }

        "WorkflowEngineEnvelope: Avro Schema should be backward compatible" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(
              WorkflowEngineEnvelope::class,
              WorkflowEngineEnvelope.serializer(),
          )

          checkBackwardCompatibility(
              WorkflowEngineEnvelope::class,
              WorkflowEngineEnvelope.serializer(),
          )
        }

        AvroSerDe.getAllSchemas(WorkflowEngineEnvelope::class)
            .forEach { (version, schema) ->
              "WorkflowEngineEnvelope: We should be able to read binary from previous version $version" {
                val bytes = AvroSerDe.getRandomBinary(schema)
                val e = shouldThrowAny { WorkflowEngineEnvelope.fromByteArray(bytes, schema) }
                e::class shouldBeOneOf listOf(
                    // IllegalArgumentException is thrown because we have more than 1 message in the envelope
                    IllegalArgumentException::class,
                    // NullPointerException is thrown because message() can be null
                    NullPointerException::class,
                )
              }
            }
      },
  )
