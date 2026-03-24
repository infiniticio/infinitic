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
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.producers.BufferedInfiniticProducer
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.ContinueWorkflowTagFanout
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RemoveTagFromWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.common.workflows.tags.storage.WorkflowIdsPage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.storage.data.Bytes
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.delay
import net.bytebuddy.utility.RandomString
import kotlin.random.Random

class WorkflowTagEngineTests : StringSpec(
    {
      "public ByTag messages should enqueue an internal continuation" {
        val workflowIds = setOf(WorkflowId(), WorkflowId())
        val message = random<RetryTasksByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds)

        harness.engine.process(message, MillisInstant.now())

        coVerify(exactly = 0) { harness.storage.getWorkflowIds(message.workflowTag, message.workflowName) }
        coVerify(exactly = 0) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, any(), any())
        }
        harness.tagMessages.single().shouldBeInstanceOf<ContinueWorkflowTagFanout>()
        harness.workflowCmdMessages.size shouldBe 0

        val continuation = harness.tagMessages.single() as ContinueWorkflowTagFanout
        continuation.limit shouldBe 1000
        continuation.command().shouldBeInstanceOf<RetryTasksByTag>()
      }

      "CancelWorkflowByTag continuation should fan out one CancelWorkflow per workflow id" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message = random<CancelWorkflowByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds.toSet())

        harness.engine.process(message, MillisInstant.now())
        val continuation = harness.tagMessages.single() as ContinueWorkflowTagFanout

        harness.tagMessages.clear()
        harness.engine.process(continuation, MillisInstant.now())

        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1000, null)
        }
        harness.workflowCmdMessages.size shouldBe workflowIds.size
        harness.workflowCmdMessages.filterIsInstance<CancelWorkflow>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
        harness.tagMessages.size shouldBe 0
      }

      "RetryWorkflowTaskByTag continuation should fan out one RetryWorkflowTask per workflow id" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message = random<RetryWorkflowTaskByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds.toSet())

        harness.engine.process(message, MillisInstant.now())
        val continuation = harness.tagMessages.single() as ContinueWorkflowTagFanout

        harness.tagMessages.clear()
        harness.engine.process(continuation, MillisInstant.now())

        harness.workflowCmdMessages.filterIsInstance<RetryWorkflowTask>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
      }

      "SendSignalByTag continuation should request the next page when cursor remains" {
        val workflowId = WorkflowId()
        val message = random<SendSignalByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, setOf(workflowId))
        val firstPage = WorkflowIdsPage(workflowIds = listOf(workflowId), nextCursor = "cursor-2")
        coEvery {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1000, null)
        } returns firstPage

        harness.engine.process(message, MillisInstant.now())
        val continuation = harness.tagMessages.single() as ContinueWorkflowTagFanout

        harness.tagMessages.clear()
        harness.engine.process(continuation, MillisInstant.now())

        harness.workflowCmdMessages.single().shouldBeInstanceOf<SendSignal>()
        harness.tagMessages.single().shouldBeInstanceOf<ContinueWorkflowTagFanout>()
        (harness.tagMessages.single() as ContinueWorkflowTagFanout).cursor shouldBe "cursor-2"
      }

      "SendSignalByTag should fan out all pages until completion" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message = random<SendSignalByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds.toSet())

        coEvery {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1000, null)
        } returns WorkflowIdsPage(
            workflowIds = listOf(workflowIds.first()),
            nextCursor = "cursor-2",
        )
        coEvery {
          harness.storage.getWorkflowIdsPage(
              message.workflowTag,
              message.workflowName,
              1000,
              "cursor-2",
          )
        } returns WorkflowIdsPage(
            workflowIds = listOf(workflowIds.last()),
            nextCursor = null,
        )

        harness.engine.process(message, MillisInstant.now())
        val firstContinuation = harness.tagMessages.single() as ContinueWorkflowTagFanout

        harness.tagMessages.clear()
        harness.engine.process(firstContinuation, MillisInstant.now())
        harness.workflowCmdMessages.filterIsInstance<SendSignal>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(listOf(workflowIds.first()))
        val secondContinuation = harness.tagMessages.single() as ContinueWorkflowTagFanout
        secondContinuation.cursor shouldBe "cursor-2"

        harness.tagMessages.clear()
        harness.engine.process(secondContinuation, MillisInstant.now())

        harness.workflowCmdMessages.filterIsInstance<SendSignal>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
        harness.tagMessages shouldBe emptyList()

        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1000, null)
        }
        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1000, "cursor-2")
        }
      }

      "getWorkflowIdsByTag should still read the full set and reply to the client" {
        val message = random<GetWorkflowIdsByTag>()
        val workflowIds = setOf(WorkflowId(), WorkflowId())
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds)

        harness.engine.process(message, MillisInstant.now())

        coVerify(exactly = 1) { harness.storage.getWorkflowIds(message.workflowTag, message.workflowName) }
        harness.clientMessages.single().shouldBeInstanceOf<WorkflowIdsByTag>()
        (harness.clientMessages.single() as WorkflowIdsByTag).workflowIds shouldBe workflowIds
      }

      "batch processing should do the same than one by one processing" {
        val n = 3
        val tags = List(n) { RandomString.make(10) }
        val names = List(n) { RandomString.make(10) }
        val ids = List(100) { RandomString.make(10) }
        val messages: List<WorkflowTagEngineMessage> = List(100) {
          val common: Map<String, Any?> = mapOf(
              "workflowTag" to WorkflowTag(tags[Random.nextInt(n)]),
              "workflowName" to WorkflowName(names[Random.nextInt(n)]),
          )

          when (Random.nextInt(8)) {
            0 -> random<AddTagToWorkflow>(common)
            1 -> random<RemoveTagFromWorkflow>(common)
            2 -> random<SendSignalByTag>(common)
            3 -> random<CancelWorkflowByTag>(common)
            4 -> random<RetryWorkflowTaskByTag>(common)
            5 -> random<RetryTasksByTag>(common)
            6 -> random<CompleteTimersByTag>(common)
            else -> random<DispatchMethodByTag>(common)
          }
        }

        val publishedAt = List(messages.size) { delay(1); MillisInstant.now() }
        fun randomTag() = WorkflowTag(tags[Random.nextInt(n)])
        fun randomId() = WorkflowId(ids[Random.nextInt(100)])
        fun randomName() = WorkflowName(names[Random.nextInt(n)])

        val keySetStorage1 = mutableMapOf<String, MutableSet<Bytes>>()
        val storage1 = BinaryWorkflowTagStorage(InMemoryKeySetStorage(keySetStorage1))
        repeat(100) {
          storage1.addWorkflowId(randomTag(), randomName(), randomId())
        }

        val keySetStorage2: MutableMap<String, MutableSet<Bytes>> =
            keySetStorage1.mapValues { entry -> entry.value.toMutableSet() }.toMutableMap()

        val producer1 = BufferedInfiniticProducer(getProducer().producer)
        val engine1 = WorkflowTagEngine(storage1, producer1)
        engine1.batchProcess(messages.zip(publishedAt))

        val storage2 = BinaryWorkflowTagStorage(InMemoryKeySetStorage(keySetStorage2))
        val producer2 = BufferedInfiniticProducer(getProducer().producer)
        val engine2 = WorkflowTagEngine(storage2, producer2)

        messages.zip(publishedAt).forEach { (message, emittedAt) ->
          engine2.process(message, emittedAt)
        }

        compareKeySetStorage(keySetStorage1, keySetStorage2) shouldBe true
        (producer1.buffer.toSet() == producer2.buffer.toSet()) shouldBe true
      }
    },
)

private data class ProducerHarness(
  val producer: InfiniticProducer,
  val workflowCmdMessages: MutableList<WorkflowStateEngineMessage>,
  val clientMessages: MutableList<ClientMessage>,
  val tagMessages: MutableList<WorkflowTagEngineMessage>,
)

private data class Harness(
  val engine: WorkflowTagEngine,
  val storage: WorkflowTagStorage,
  val producer: InfiniticProducer,
  val workflowCmdMessages: MutableList<WorkflowStateEngineMessage>,
  val clientMessages: MutableList<ClientMessage>,
  val tagMessages: MutableList<WorkflowTagEngineMessage>,
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
  workflowIds: Set<WorkflowId>,
): WorkflowTagStorage {
  val tagStateStorage = mockk<WorkflowTagStorage>()
  val sortedIds = workflowIds.sortedBy { it.toString() }

  coEvery { tagStateStorage.getWorkflowIds(workflowTag, workflowName) } returns workflowIds
  coEvery {
    tagStateStorage.getWorkflowIdsPage(workflowTag, workflowName, any(), any())
  } answers {
    WorkflowIdsPage(
        workflowIds = sortedIds,
        nextCursor = null,
    )
  }
  coEvery { tagStateStorage.getWorkflowIds(any<Set<Pair<WorkflowTag, WorkflowName>>>()) } returns mapOf(
      (workflowTag to workflowName) to workflowIds,
  )
  coEvery { tagStateStorage.updateWorkflowIds(any(), any()) } just Runs
  coEvery { tagStateStorage.addWorkflowId(any(), any(), any()) } just Runs
  coEvery { tagStateStorage.removeWorkflowId(any(), any(), any()) } just Runs

  return tagStateStorage
}

private fun getHarness(
  workflowTag: WorkflowTag,
  workflowName: WorkflowName,
  workflowIds: Set<WorkflowId> = setOf(WorkflowId()),
): Harness {
  val storage = mockWorkflowTagStorage(workflowTag, workflowName, workflowIds)
  val producerHarness = getProducer()

  return Harness(
      engine = WorkflowTagEngine(storage, producerHarness.producer),
      storage = storage,
      producer = producerHarness.producer,
      workflowCmdMessages = producerHarness.workflowCmdMessages,
      clientMessages = producerHarness.clientMessages,
      tagMessages = producerHarness.tagMessages,
  )
}

private fun getProducer() = ProducerHarness(
    workflowCmdMessages = mutableListOf(),
    clientMessages = mutableListOf(),
    tagMessages = mutableListOf(),
    producer = mockk(),
).let { harness ->
  val producer = mockk<InfiniticProducer> {
    every { emitterName } returns EmitterName("clientWorkflowTagEngineName")
    coEvery { capture(harness.clientMessages).sendTo(ClientTopic) } returns Unit
    coEvery { capture(harness.workflowCmdMessages).sendTo(WorkflowStateCmdTopic) } returns Unit
    coEvery { capture(harness.tagMessages).sendTo(WorkflowTagEngineTopic) } returns Unit
  }

  harness.copy(producer = producer)
}
