/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
package io.infinitic.clients

import io.infinitic.clients.config.InfiniticClientConfig
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.Base64
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class InfiniticClientGetWorkflowStateTest :
  StringSpec(
      {
        val transport = InMemoryTransportConfig()
        val storage = InMemoryStorageConfig.builder().build()

        val configWithStorage =
            InfiniticClientConfig(name = "test-client", transport = transport, storage = storage)

        val configWithoutStorage =
            InfiniticClientConfig(name = "test-client-no-storage", transport = transport)

        val clientWithStorage = InfiniticClient(configWithStorage)
        val clientWithoutStorage = InfiniticClient(configWithoutStorage)

        afterSpec {
          clientWithStorage.close()
          clientWithoutStorage.close()
        }

        "getWorkflowStateJson should throw when storage is not configured" {
          val exception = shouldThrow<java.util.concurrent.CompletionException> {
            clientWithoutStorage.getWorkflowStateJsonById(IdGenerator.next())
          }
          exception.cause.shouldBeInstanceOf<IllegalStateException>()
        }

        "getWorkflowStateJsonAsync should throw when storage is not configured" {
          shouldThrow<IllegalStateException> {
            clientWithoutStorage.getWorkflowStateJsonByIdAsync(IdGenerator.next()).await()
          }
        }

        "getWorkflowStateJson should return null for non-existent workflow" {
          val workflowId = IdGenerator.next()
          val state = clientWithStorage.getWorkflowStateJsonById(workflowId)

          state.shouldBeNull()
        }

        "getWorkflowStateJsonAsync should return null for non-existent workflow" {
          val workflowId = IdGenerator.next()
          val state = clientWithStorage.getWorkflowStateJsonByIdAsync(workflowId).await()

          state.shouldBeNull()
        }

        "getWorkflowStateJson should return json for existing workflow" {
          val workflowId = WorkflowId(IdGenerator.next())
          val workflowName = WorkflowName("TestWorkflow")
          val propertyValue = PropertyValue.from("property-value", String::class.java)
          val propertyName = PropertyName("prop")
          val propertyHash = propertyValue.hash()
          val workflowMetaValue = "meta-value".toByteArray()

          val expectedState =
              WorkflowState(
                  lastMessageId = MessageId(IdGenerator.next()),
                  workflowId = workflowId,
                  workflowName = workflowName,
                  workflowVersion = WorkflowVersion(1),
                  workflowTags = setOf(),
                  workflowMeta = WorkflowMeta(mapOf("meta-key" to workflowMetaValue)),
                  runningWorkflowTaskId = null,
                  runningWorkflowMethodId = null,
                  positionInRunningWorkflowMethod = null,
                  workflowMethods = mutableListOf(
                      WorkflowMethod(
                          waitingClients = mutableSetOf(),
                          workflowMethodId = WorkflowMethodId("test-method-id"),
                          requester = null,
                          methodName = MethodName("run"),
                          methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
                          methodParameters = MethodArgs(
                              listOf(SerializedData.encode("arg-value", String::class.java, null)),
                          ),
                          methodReturnValue = MethodReturnValue.from(
                              "return-value",
                              String::class.java,
                          ),
                          workflowTaskIndexAtStart = WorkflowTaskIndex(0),
                          propertiesNameHashAtStart = mapOf(propertyName to propertyHash),
                      ),
                  ),
                  currentPropertiesNameHash = mutableMapOf(propertyName to propertyHash),
                  propertiesHashValue = mutableMapOf(propertyHash to propertyValue),
              )

          val workflowStorage = BinaryWorkflowStateStorage(storage.keyValue)
          workflowStorage.putStateWithVersion(workflowId, expectedState, 0)

          val retrievedState = clientWithStorage.getWorkflowStateJsonById(workflowId.toString())
          val retrievedJson = Json.parseToJsonElement(retrievedState!!).jsonObject
          val workflowMethodJson = retrievedJson["workflowMethods"]!!.jsonArray[0].jsonObject
          val methodParametersJson = workflowMethodJson["methodParameters"]!!.jsonArray
          val workflowMetaJson = retrievedJson["workflowMeta"]!!.jsonObject
          val propertiesJson = retrievedJson["propertiesHashValue"]!!.jsonObject

          workflowMetaJson["meta-key"] shouldBe
              JsonPrimitive(Base64.getEncoder().encodeToString(workflowMetaValue))
          methodParametersJson[0] shouldBe JsonPrimitive("arg-value")
          workflowMethodJson["methodReturnValue"] shouldBe JsonPrimitive("return-value")
          propertiesJson[propertyHash.hash] shouldBe JsonPrimitive("property-value")
        }

        "getWorkflowStateJsonAsync should return json for existing workflow" {
          val workflowId = WorkflowId(IdGenerator.next())
          val workflowName = WorkflowName("TestWorkflow")

          val expectedState =
              WorkflowState(
                  lastMessageId = MessageId(IdGenerator.next()),
                  workflowId = workflowId,
                  workflowName = workflowName,
                  workflowVersion = WorkflowVersion(1),
                  workflowTags = setOf(),
                  workflowMeta = WorkflowMeta(),
                  runningWorkflowTaskId = null,
                  runningWorkflowMethodId = null,
                  positionInRunningWorkflowMethod = null,
                  workflowMethods = mutableListOf(),
              )

          val workflowStorage = BinaryWorkflowStateStorage(storage.keyValue)
          workflowStorage.putStateWithVersion(workflowId, expectedState, 0)

          val retrievedState =
              clientWithStorage.getWorkflowStateJsonByIdAsync(workflowId.toString()).await()

          retrievedState shouldBe expectedState.toClientJson()
        }

        "getWorkflowStateJsonSuspend should throw when storage is not configured" {
          shouldThrow<IllegalStateException> {
            clientWithoutStorage.getWorkflowStateJsonByIdSuspend(IdGenerator.next())
          }
        }

        "getWorkflowStateJsonSuspend should return null for non-existent workflow" {
          val workflowId = IdGenerator.next()
          val state = clientWithStorage.getWorkflowStateJsonByIdSuspend(workflowId)

          state.shouldBeNull()
        }

        "getWorkflowStateJsonSuspend should return json for existing workflow" {
          val workflowId = WorkflowId(IdGenerator.next())
          val workflowName = WorkflowName("TestWorkflow")

          val expectedState =
              WorkflowState(
                  lastMessageId = MessageId(IdGenerator.next()),
                  workflowId = workflowId,
                  workflowName = workflowName,
                  workflowVersion = WorkflowVersion(1),
                  workflowTags = setOf(),
                  workflowMeta = WorkflowMeta(),
                  runningWorkflowTaskId = null,
                  runningWorkflowMethodId = null,
                  positionInRunningWorkflowMethod = null,
                  workflowMethods = mutableListOf(),
              )

          val workflowStorage = BinaryWorkflowStateStorage(storage.keyValue)
          workflowStorage.putStateWithVersion(workflowId, expectedState, 0)

          val retrievedState =
              clientWithStorage.getWorkflowStateJsonByIdSuspend(workflowId.toString())

          retrievedState shouldBe expectedState.toClientJson()
        }
      },
  )
