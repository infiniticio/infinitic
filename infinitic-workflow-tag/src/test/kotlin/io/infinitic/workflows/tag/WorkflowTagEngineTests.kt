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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.producers.BufferedInfiniticProducer
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
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
import net.bytebuddy.utility.RandomString
import kotlin.random.Random

class WorkflowTagEngineTests : StringSpec(
    {
      "public ByTag messages should fan out the first page immediately" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message = random<RetryTasksByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds.toSet())

        harness.engine.process(message, MillisInstant.now())

        coVerify(exactly = 0) { harness.storage.getWorkflowIds(message.workflowTag, message.workflowName) }
        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 5000, null)
        }
        harness.tagMessages shouldBe emptyList()
        harness.workflowCmdMessages.filterIsInstance<RetryTasks>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
      }

      "CancelWorkflowByTag continuation should fan out one CancelWorkflow per workflow id" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message = random<CancelWorkflowByTag>()
        val harness = getHarness(message.workflowTag, message.workflowName, workflowIds.toSet())
        val emittedAt = MillisInstant.now()
        val continuation = ContinueWorkflowTagFanout.from(
            operationId = message.messageId,
            limit = 5000,
            command = message.copy(emittedAt = emittedAt),
            emitterName = message.emitterName,
            emittedAt = emittedAt,
        )

        harness.engine.process(continuation, MillisInstant.now())

        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 5000, null)
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
        val emittedAt = MillisInstant.now()
        val continuation = ContinueWorkflowTagFanout.from(
            operationId = message.messageId,
            limit = 5000,
            command = message.copy(emittedAt = emittedAt),
            emitterName = message.emitterName,
            emittedAt = emittedAt,
        )

        harness.engine.process(continuation, MillisInstant.now())

        harness.workflowCmdMessages.filterIsInstance<RetryWorkflowTask>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
      }

      "SendSignalByTag should request the next page when cursor remains" {
        val workflowId = WorkflowId()
        val message = random<SendSignalByTag>()
        val harness = getHarness(
            message.workflowTag,
            message.workflowName,
            setOf(workflowId),
            fanoutPageSize = 1,
        )
        val firstPage = WorkflowIdsPage(workflowIds = listOf(workflowId), nextCursor = "cursor-2")
        coEvery {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1, null)
        } returns firstPage

        harness.engine.process(message, MillisInstant.now())

        harness.workflowCmdMessages.single().shouldBeInstanceOf<SendSignal>()
        harness.tagMessages.single().shouldBeInstanceOf<ContinueWorkflowTagFanout>()
        (harness.tagMessages.single() as ContinueWorkflowTagFanout).cursor shouldBe "cursor-2"
      }

      "SendSignalByTag should fan out all pages until completion" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message = random<SendSignalByTag>()
        val harness = getHarness(
            message.workflowTag,
            message.workflowName,
            workflowIds.toSet(),
            fanoutPageSize = 1,
        )

        coEvery {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1, null)
        } returns WorkflowIdsPage(
            workflowIds = listOf(workflowIds.first()),
            nextCursor = "cursor-2",
        )
        coEvery {
          harness.storage.getWorkflowIdsPage(
              message.workflowTag,
              message.workflowName,
              1,
              "cursor-2",
          )
        } returns WorkflowIdsPage(
            workflowIds = listOf(workflowIds.last()),
            nextCursor = null,
        )

        harness.engine.process(message, MillisInstant.now())
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
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1, null)
        }
        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message.workflowTag, message.workflowName, 1, "cursor-2")
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

      "batch GetWorkflowIdsByTag should use buffered tag mutations" {
        val workflowTag = WorkflowTag("tag")
        val workflowName = WorkflowName("workflow")
        val workflowId = WorkflowId("workflow-id")
        val addId = WorkflowId("add-id")
        val removeId = WorkflowId("remove-id")
        val add = AddTagToWorkflow(
            workflowName = workflowName,
            workflowTag = workflowTag,
            workflowId = addId,
            emitterName = EmitterName("emitter"),
            emittedAt = null,
        )
        val remove = RemoveTagFromWorkflow(
            workflowName = workflowName,
            workflowTag = workflowTag,
            workflowId = removeId,
            emitterName = EmitterName("emitter"),
            emittedAt = null,
        )
        val query = random<GetWorkflowIdsByTag>(
            mapOf("workflowTag" to workflowTag, "workflowName" to workflowName),
        )
        val harness = getHarness(workflowTag, workflowName, setOf(workflowId, removeId))

        harness.engine.batchProcess(
            listOf(
                add to MillisInstant(1),
                remove to MillisInstant(2),
                query to MillisInstant(3),
            ),
        )

        coVerify(exactly = 1) { harness.storage.getWorkflowIds(setOf(workflowTag to workflowName)) }
        coVerify(exactly = 0) { harness.storage.getWorkflowIds(workflowTag, workflowName) }
        (harness.clientMessages.single() as WorkflowIdsByTag).workflowIds shouldBe setOf(
            workflowId,
            addId,
        )
      }

      "batch DispatchWorkflowByCustomId should use buffered custom-id mutation" {
        val customId = WorkflowTag("customId:workflow")
        val workflowName = WorkflowName("workflow")
        val firstWorkflowId = WorkflowId("first-workflow-id")
        val secondWorkflowId = WorkflowId("second-workflow-id")
        val firstDispatch = DispatchWorkflowByCustomId(
            workflowName = workflowName,
            workflowTag = customId,
            workflowId = firstWorkflowId,
            methodName = MethodName("method"),
            methodParameters = MethodArgs(),
            methodParameterTypes = null,
            workflowTags = setOf(customId),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(ClientName("client")),
            clientWaiting = false,
            emitterName = EmitterName("emitter"),
            emittedAt = null,
        )
        val secondDispatch = firstDispatch.copy(workflowId = secondWorkflowId)
        val harness = getHarness(customId, workflowName, emptySet())

        harness.engine.batchProcess(
            listOf(
                firstDispatch to MillisInstant(1),
                secondDispatch to MillisInstant(2),
            ),
        )

        coVerify(exactly = 1) { harness.storage.getWorkflowIds(setOf(customId to workflowName)) }
        coVerify(exactly = 1) {
          harness.storage.updateWorkflowIds(
              add = mapOf((customId to workflowName) to setOf(firstWorkflowId)),
              remove = emptyMap(),
          )
        }
        harness.workflowCmdMessages.filterIsInstance<DispatchWorkflow>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(listOf(firstWorkflowId))
      }

      "mixed batch should buffer tag mutations and flush them with updateWorkflowIds" {
        val workflowTag = WorkflowTag("tag")
        val workflowName = WorkflowName("workflow")
        val workflowId = WorkflowId("workflow-id")
        val addId = WorkflowId("add-id")
        val removeId = WorkflowId("remove-id")
        val common = mapOf("workflowTag" to workflowTag, "workflowName" to workflowName)
        val add = AddTagToWorkflow(
            workflowName = workflowName,
            workflowTag = workflowTag,
            workflowId = addId,
            emitterName = EmitterName("emitter"),
            emittedAt = null,
        )
        val remove = RemoveTagFromWorkflow(
            workflowName = workflowName,
            workflowTag = workflowTag,
            workflowId = removeId,
            emitterName = EmitterName("emitter"),
            emittedAt = null,
        )
        val retry = random<RetryWorkflowTaskByTag>(common)
        val harness = getHarness(workflowTag, workflowName, setOf(workflowId))

        harness.engine.batchProcess(
            listOf(
                add to MillisInstant(1),
                retry to MillisInstant(2),
                remove to MillisInstant(3),
            ),
        )

        coVerify(exactly = 0) { harness.storage.addWorkflowId(any(), any(), any()) }
        coVerify(exactly = 0) { harness.storage.removeWorkflowId(any(), any(), any()) }
        coVerify(exactly = 1) {
          harness.storage.updateWorkflowIds(
              add = mapOf((workflowTag to workflowName) to setOf(addId)),
              remove = mapOf((workflowTag to workflowName) to setOf(removeId)),
          )
        }
        harness.workflowCmdMessages.filterIsInstance<RetryWorkflowTask>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(listOf(workflowId))
      }

      "large mixed batch should keep storage interactions batched" {
        val workflowTag = WorkflowTag("tag")
        val workflowName = WorkflowName("workflow")
        val existingId = WorkflowId("existing-id")
        val pageIds = listOf(WorkflowId("page-1"), WorkflowId("page-2"))
        val addIds = (1..100).map { WorkflowId("add-$it") }
        val removeIds = (1..100).map { WorkflowId("remove-$it") }
        val common = mapOf("workflowTag" to workflowTag, "workflowName" to workflowName)
        val adds = addIds.map { workflowId ->
          AddTagToWorkflow(
              workflowName = workflowName,
              workflowTag = workflowTag,
              workflowId = workflowId,
              emitterName = EmitterName("emitter"),
              emittedAt = null,
          )
        }
        val removes = removeIds.map { workflowId ->
          RemoveTagFromWorkflow(
              workflowName = workflowName,
              workflowTag = workflowTag,
              workflowId = workflowId,
              emitterName = EmitterName("emitter"),
              emittedAt = null,
          )
        }
        val retryWorkflowTask = random<RetryWorkflowTaskByTag>(common)
        val retryTasks = random<RetryTasksByTag>(common)
        val query = random<GetWorkflowIdsByTag>(common)
        val harness = getHarness(
            workflowTag,
            workflowName,
            setOf(existingId) + removeIds.toSet(),
        )
        coEvery {
          harness.storage.getWorkflowIdsPage(workflowTag, workflowName, 5000, null)
        } returns WorkflowIdsPage(pageIds, null)

        val messages = buildList {
          adds.forEachIndexed { index, message -> add(message to MillisInstant(index.toLong())) }
          removes.forEachIndexed { index, message ->
            add(message to MillisInstant((adds.size + index).toLong()))
          }
          add(retryWorkflowTask to MillisInstant(300))
          add(retryTasks to MillisInstant(301))
          add(query to MillisInstant(302))
        }

        harness.engine.batchProcess(messages)

        coVerify(exactly = 0) { harness.storage.addWorkflowId(any(), any(), any()) }
        coVerify(exactly = 0) { harness.storage.removeWorkflowId(any(), any(), any()) }
        coVerify(exactly = 1) { harness.storage.getWorkflowIds(setOf(workflowTag to workflowName)) }
        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(workflowTag, workflowName, 5000, null)
        }
        coVerify(exactly = 1) {
          harness.storage.updateWorkflowIds(
              add = mapOf((workflowTag to workflowName) to addIds.toSet()),
              remove = mapOf((workflowTag to workflowName) to removeIds.toSet()),
          )
        }
        (harness.clientMessages.single() as WorkflowIdsByTag).workflowIds shouldBe
            setOf(existingId) + addIds.toSet()
        harness.workflowCmdMessages.filterIsInstance<RetryWorkflowTask>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(pageIds)
        harness.workflowCmdMessages.filterIsInstance<RetryTasks>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(pageIds)
      }

      "fan-outs in the same batch should share page reads for the same cursor and limit" {
        val workflowIds = listOf(WorkflowId(), WorkflowId())
        val message1 = random<RetryWorkflowTaskByTag>()
        val common = mapOf(
            "workflowTag" to message1.workflowTag,
            "workflowName" to message1.workflowName,
        )
        val message2 = random<RetryTasksByTag>(common)
        val harness = getHarness(message1.workflowTag, message1.workflowName, workflowIds.toSet())

        harness.engine.batchProcess(
            listOf(
                message1 to MillisInstant(1),
                message2 to MillisInstant(2),
            ),
        )

        coVerify(exactly = 1) {
          harness.storage.getWorkflowIdsPage(message1.workflowTag, message1.workflowName, 5000, null)
        }
        coVerify(exactly = 0) { harness.storage.updateWorkflowIds(any(), any()) }
        harness.workflowCmdMessages.filterIsInstance<RetryWorkflowTask>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
        harness.workflowCmdMessages.filterIsInstance<RetryTasks>().map { it.workflowId }
            .shouldContainExactlyInAnyOrder(workflowIds)
      }

      "batch processing should do the same than one by one processing for paginated fan-out" {
        val n = 3
        val tags = List(n) { RandomString.make(10) }
        val names = List(n) { RandomString.make(10) }
        val ids = List(100) { RandomString.make(10) }
        val messages: List<WorkflowTagEngineMessage> = List(100) {
          val common: Map<String, Any?> = mapOf(
              "workflowTag" to WorkflowTag(tags[Random.nextInt(n)]),
              "workflowName" to WorkflowName(names[Random.nextInt(n)]),
          )

          when (Random.nextInt(6)) {
            0 -> random<SendSignalByTag>(common)
            1 -> random<CancelWorkflowByTag>(common)
            2 -> random<RetryWorkflowTaskByTag>(common)
            3 -> random<RetryTasksByTag>(common)
            4 -> random<CompleteTimersByTag>(common)
            else -> random<DispatchMethodByTag>(common)
          }
        }

        val publishedAt = messages.indices.map { MillisInstant(it.toLong()) }
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
        val engine1 = WorkflowTagEngine(storage1, producer1, fanoutPageSize = 7)
        engine1.batchProcess(messages.zip(publishedAt))

        val storage2 = BinaryWorkflowTagStorage(InMemoryKeySetStorage(keySetStorage2))
        val producer2 = BufferedInfiniticProducer(getProducer().producer)
        val engine2 = WorkflowTagEngine(storage2, producer2, fanoutPageSize = 7)

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
  fanoutPageSize: Int = 5000,
): Harness {
  val storage = mockWorkflowTagStorage(workflowTag, workflowName, workflowIds)
  val producerHarness = getProducer()

  return Harness(
      engine = WorkflowTagEngine(storage, producerHarness.producer, fanoutPageSize),
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
