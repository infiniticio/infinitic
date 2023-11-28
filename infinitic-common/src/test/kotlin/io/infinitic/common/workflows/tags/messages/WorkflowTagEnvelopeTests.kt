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
package io.infinitic.common.workflows.tags.messages

import io.infinitic.common.checkBackwardCompatibility
import io.infinitic.common.checkOrCreateCurrentFile
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class WorkflowTagEnvelopeTests :
  StringSpec(
      {
        WorkflowTagMessage::class.sealedSubclasses.map {
          val msg =
              when (it) {
                DispatchWorkflowByCustomId::class ->
                  TestFactory.random(
                      it,
                      mapOf(
                          "workflowTag" to
                              WorkflowTag(
                                  WorkflowTag.CUSTOM_ID_PREFIX +
                                      TestFactory.random(String::class),
                              ),
                      ),
                  )

                else -> TestFactory.random(it)
              }

          "WorkflowTagMessage(${msg::class.simpleName}) should be avro-convertible" {
            shouldNotThrowAny {
              val envelope = WorkflowTagEnvelope.from(msg)
              val bytes: ByteArray = envelope.toByteArray()

              WorkflowTagEnvelope.fromByteArray(bytes, WorkflowTagEnvelope.writerSchema) shouldBe
                  envelope
            }
          }
        }

        "Existing schema file should be up-to-date with the current version" {
          // An error in this test means that we need to upgrade the version
          checkOrCreateCurrentFile(WorkflowTagEnvelope.serializer())
        }

        "Avro Schema should be backward compatible to 0.9.0" {
          checkBackwardCompatibility(WorkflowTagEnvelope.serializer())
        }
      },
  )
