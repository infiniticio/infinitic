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
package io.infinitic.workflows.tag

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.producers.BufferedInfiniticProducer
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.storage.data.Bytes
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.delay
import net.bytebuddy.utility.RandomString
import kotlin.random.Random

private fun <T : Any> captured(slot: CapturingSlot<T>) =
    if (slot.isCaptured) slot.captured else null

private val stateWorkflowId = slot<String>()
private val workflowEngineSlot = slot<WorkflowStateEngineMessage>()
private val clientSlot = slot<ClientMessage>()

private lateinit var producer: InfiniticProducer
private lateinit var workflowTagStorage: WorkflowTagStorage

private class WorkflowTagEngineTests :
  StringSpec(
      {
        "cancelWorkflowPerTag should cancel workflow" {
          // given
          val workflowIds = setOf(WorkflowId(), WorkflowId())
          val msgIn = random<CancelWorkflowByTag>()
          // when
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).process(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producer.emitterName
            with(producer) { ofType<CancelWorkflow>().sendTo(WorkflowStateEngineTopic) }
            with(producer) { ofType<CancelWorkflow>().sendTo(WorkflowStateEngineTopic) }
          }
          confirmVerified()
          // checking last message
          val cancelWorkflow = captured(workflowEngineSlot)!! as CancelWorkflow

          with(cancelWorkflow) { workflowId shouldBe workflowIds.last() }
        }

        "RetryWorkflowTaskByTag should retry workflow task" {
          // given
          val workflowIds = setOf(WorkflowId(), WorkflowId())
          val msgIn = random<RetryWorkflowTaskByTag>()
          // when
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).process(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producer.emitterName
            with(producer) {
              ofType<RetryWorkflowTask>().sendTo(WorkflowStateEngineTopic)
            }
            with(producer) {
              ofType<RetryWorkflowTask>().sendTo(
                  WorkflowStateEngineTopic,
              )
            }
          }
          confirmVerified()
          // checking last message
          with(captured(workflowEngineSlot)!! as RetryWorkflowTask) {
            workflowId shouldBe workflowIds.last()
            workflowName shouldBe msgIn.workflowName
          }
        }

        "RetryTasksByTag should retry tasks" {
          // given
          val workflowIds = setOf(WorkflowId(), WorkflowId())
          val msgIn = random<RetryTasksByTag>()
          // when
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).process(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producer.emitterName
            with(producer) { ofType<RetryTasks>().sendTo(WorkflowStateEngineTopic) }
            with(producer) { ofType<RetryTasks>().sendTo(WorkflowStateEngineTopic) }
          }
          confirmVerified()
          // checking last message
          with(captured(workflowEngineSlot)!! as RetryTasks) {
            workflowId shouldBe workflowIds.last()
            workflowName shouldBe msgIn.workflowName
            taskId shouldBe msgIn.taskId
            taskStatus shouldBe msgIn.taskStatus
            serviceName shouldBe msgIn.serviceName
          }
        }

        "CompleteTimerByTag should complete timer" {
          // given
          val workflowIds = setOf(WorkflowId(), WorkflowId())
          val msgIn = random<CompleteTimersByTag>()
          // when
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).process(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producer.emitterName
            with(producer) { ofType<CompleteTimers>().sendTo(WorkflowStateEngineTopic) }
            with(producer) { ofType<CompleteTimers>().sendTo(WorkflowStateEngineTopic) }
          }
          confirmVerified()
          // checking last message
          with(captured(workflowEngineSlot)!! as CompleteTimers) {
            workflowId shouldBe workflowIds.last()
            workflowName shouldBe msgIn.workflowName
            workflowMethodId shouldBe msgIn.workflowMethodId
          }
        }

        "sendToChannelPerTag should send to channel" {
          // given
          val workflowIds = setOf(WorkflowId(), WorkflowId())
          val msgIn = random<SendSignalByTag>()
          // when
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).process(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producer.emitterName
            with(producer) { ofType<SendSignal>().sendTo(WorkflowStateEngineTopic) }
            with(producer) { ofType<SendSignal>().sendTo(WorkflowStateEngineTopic) }
          }
          confirmVerified()
          // checking last message
          val sendSignal = captured(workflowEngineSlot)!! as SendSignal

          with(sendSignal) {
            workflowId shouldBe workflowIds.last()
            workflowName shouldBe msgIn.workflowName
            signalData shouldBe msgIn.signalData
            signalId shouldBe msgIn.signalId
            signalData shouldBe msgIn.signalData
            channelTypes shouldBe msgIn.channelTypes
            channelName shouldBe msgIn.channelName
          }
        }

        "getWorkflowIdsPerTag should return set of ids" {
          // given
          val msgIn = random<GetWorkflowIdsByTag>()
          val workflowId1 = WorkflowId()
          val workflowId2 = WorkflowId()
          // when
          getEngine(
              msgIn.workflowTag,
              msgIn.workflowName,
              workflowIds = setOf(workflowId1, workflowId2),
          )
              .process(msgIn, MillisInstant.now())
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producer.emitterName
            with(producer) { ofType<WorkflowIdsByTag>().sendTo(ClientTopic) }
          }
          confirmVerified()

          captured(clientSlot).shouldBeInstanceOf<WorkflowIdsByTag>()
          (captured(clientSlot) as WorkflowIdsByTag).workflowIds shouldBe
              setOf(workflowId1, workflowId2)
        }

        "batch processing should do the same than one by one processing" {
          val n = 3
          val tags = List(n) { RandomString.make(10) }
          val names = List(n) { RandomString.make(10) }
          val ids = List(100) { RandomString.make(10) }
          val messages = List(100) {
            random<WorkflowTagEngineMessage>(
                mapOf(
                    "workflowTag" to WorkflowTag(tags[Random.nextInt(n)]),
                    "workflowName" to WorkflowName(names[Random.nextInt(n)]),
                ),
            )
          }.mapNotNull {
            when (it) {
              is DispatchWorkflowByCustomId -> null
              else -> it
            }
          }

          val publishedAt = List(messages.size) { delay(1); MillisInstant.now() }
          fun randomTag() = WorkflowTag(tags[Random.nextInt(n)])
          fun randomId() = WorkflowId(ids[Random.nextInt(100)])
          fun randomName() = WorkflowName(names[Random.nextInt(n)])


          // Batch processing
          val keySetStorage1 = mutableMapOf<String, MutableSet<Bytes>>()
          val storage1 = BinaryWorkflowTagStorage(InMemoryKeySetStorage(keySetStorage1))
          repeat(100) {
            storage1.addWorkflowId(randomTag(), randomName(), randomId())
          }
          // copy
          val keySetStorage2: MutableMap<String, MutableSet<Bytes>> =
              keySetStorage1.mapValues { entry ->
                entry.value.toMutableSet()
              }.toMutableMap()

          val producer1 = BufferedInfiniticProducer(getProducer())
          val engine1 = WorkflowTagEngine(storage1, producer1)
          engine1.batchProcess(messages.zip(publishedAt))

          // One by One processing
          val storage2 = BinaryWorkflowTagStorage(InMemoryKeySetStorage(keySetStorage2))
          val producer2 = BufferedInfiniticProducer(getProducer())
          val engine2 = WorkflowTagEngine(storage2, producer2)

          messages.zip(publishedAt).forEach { (msg, publishedAt) ->
            engine2.process(msg, publishedAt)
          }

          // compare database final state
          compareKeySetStorage(keySetStorage1, keySetStorage2) shouldBe true
          // compare messages
          (producer1.buffer.toSet() == producer2.buffer.toSet()) shouldBe true
        }
      },
  )

private fun compareKeySetStorage(
  map1: MutableMap<String, MutableSet<Bytes>>,
  map2: MutableMap<String, MutableSet<Bytes>>
): Boolean {
  if (map1.keys != map2.keys) {
    return false
  }

  for ((key, valueSet1) in map1) {
    val valueSet2 = map2[key] ?: return false
    if (valueSet1 != valueSet2) {
      return false
    }
  }

  return true
}

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

private fun mockWorkflowTagStorage(
  workflowTag: WorkflowTag,
  workflowName: WorkflowName,
  workflowIds: Set<WorkflowId>
): WorkflowTagStorage {
  val tagStateStorage = mockk<WorkflowTagStorage>()
  coEvery { tagStateStorage.getWorkflowIds(workflowTag, workflowName) } returns workflowIds
  coEvery {
    tagStateStorage.addWorkflowId(workflowTag, workflowName, WorkflowId(capture(stateWorkflowId)))
  } just Runs
  coEvery {
    tagStateStorage.removeWorkflowId(
        workflowTag, workflowName, WorkflowId(capture(stateWorkflowId)),
    )
  } just Runs

  return tagStateStorage
}


private fun getEngine(
  workflowTag: WorkflowTag,
  workflowName: WorkflowName,
  workflowIds: Set<WorkflowId> = setOf(WorkflowId())
): WorkflowTagEngine {
  workflowTagStorage = mockWorkflowTagStorage(workflowTag, workflowName, workflowIds)

  producer = getProducer()

  return WorkflowTagEngine(workflowTagStorage, producer)
}

private fun getProducer() = mockk<InfiniticProducer> {
  every { emitterName } returns EmitterName("clientWorkflowTagEngineName")
  coEvery { capture(clientSlot).sendTo(ClientTopic) } returns Unit
  coEvery { capture(workflowEngineSlot).sendTo(WorkflowStateEngineTopic) } returns Unit
}
