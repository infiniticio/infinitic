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
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
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
import java.util.concurrent.CompletableFuture

private fun <T : Any> captured(slot: CapturingSlot<T>) =
    if (slot.isCaptured) slot.captured else null

private val stateWorkflowId = slot<String>()
private val workflowEngineSlot = slot<WorkflowEngineMessage>()
private val clientSlot = slot<ClientMessage>()

private lateinit var producerAsync: InfiniticProducerAsync
private lateinit var workflowTagStorage: WorkflowTagStorage

private class WorkflowTagEngineTests :
  StringSpec(
      {
        "cancelWorkflowPerTag should cancel workflow" {
          // given
          val workflowIds = setOf(WorkflowId(), WorkflowId())
          val msgIn = random<CancelWorkflowByTag>()
          // when
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producerAsync.producerName
            with(producerAsync) { ofType<CancelWorkflow>().sendToAsync(WorkflowEngineTopic) }
            with(producerAsync) { ofType<CancelWorkflow>().sendToAsync(WorkflowEngineTopic) }
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
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producerAsync.producerName
            with(producerAsync) {
              ofType<RetryWorkflowTask>().sendToAsync(
                  WorkflowEngineTopic,
              )
            }
            with(producerAsync) {
              ofType<RetryWorkflowTask>().sendToAsync(
                  WorkflowEngineTopic,
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
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producerAsync.producerName
            with(producerAsync) { ofType<RetryTasks>().sendToAsync(WorkflowEngineTopic) }
            with(producerAsync) { ofType<RetryTasks>().sendToAsync(WorkflowEngineTopic) }
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
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producerAsync.producerName
            with(producerAsync) { ofType<CompleteTimers>().sendToAsync(WorkflowEngineTopic) }
            with(producerAsync) { ofType<CompleteTimers>().sendToAsync(WorkflowEngineTopic) }
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
          getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producerAsync.producerName
            with(producerAsync) { ofType<SendSignal>().sendToAsync(WorkflowEngineTopic) }
            with(producerAsync) { ofType<SendSignal>().sendToAsync(WorkflowEngineTopic) }
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
              .handle(msgIn, MillisInstant.now())
          // then
          coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            producerAsync.producerName
            with(producerAsync) { ofType<WorkflowIdsByTag>().sendToAsync(ClientTopic) }
          }
          confirmVerified()

          captured(clientSlot).shouldBeInstanceOf<WorkflowIdsByTag>()
          (captured(clientSlot) as WorkflowIdsByTag).workflowIds shouldBe
              setOf(workflowId1, workflowId2)
        }
      },
  )

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

  fun completed() = CompletableFuture.completedFuture(Unit)

  producerAsync = mockk<InfiniticProducerAsync> {
    every { producerName } returns "clientWorkflowTagEngineName"
    every { capture(clientSlot).sendToAsync(ClientTopic) } returns completed()
    every { capture(workflowEngineSlot).sendToAsync(WorkflowEngineTopic) } returns completed()
  }

  return WorkflowTagEngine(workflowTagStorage, producerAsync)
}
