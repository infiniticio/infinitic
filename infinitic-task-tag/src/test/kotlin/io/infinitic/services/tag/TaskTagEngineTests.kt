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
package io.infinitic.services.tag

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.clientName
import io.infinitic.common.requester.workflowId
import io.infinitic.common.requester.workflowMethodId
import io.infinitic.common.requester.workflowMethodName
import io.infinitic.common.requester.workflowName
import io.infinitic.common.requester.workflowVersion
import io.infinitic.common.tasks.data.DelegatedTaskData
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskReturnValue
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.tags.messages.AddTaskIdToTag
import io.infinitic.common.tasks.tags.messages.CompleteDelegatedTask
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTaskIdFromTag
import io.infinitic.common.tasks.tags.messages.SetDelegatedTaskData
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.workers.data.WorkerName
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.tasks.tag.TaskTagEngine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture

private fun <T : Any> captured(slot: CapturingSlot<T>) =
    if (slot.isCaptured) slot.captured else null

private val workerName = WorkerName("clientTaskTagEngineTests")
private val taskTag = slot<TaskTag>()
private val serviceName = slot<ServiceName>()
private val taskId = slot<TaskId>()
private val clientMessage = slot<ClientMessage>()
private val workflowEngineMessage = slot<WorkflowEngineMessage>()
private var delegatedTaskData = slot<DelegatedTaskData>()

private lateinit var tagStateStorage: TaskTagStorage

private fun completed() = CompletableFuture.completedFuture(Unit)

private val producerMock = mockk<InfiniticProducerAsync> {
  every { producerName } returns "$workerName"
  coEvery { capture(clientMessage).sendToAsync(ClientTopic) } returns completed()
  coEvery { capture(workflowEngineMessage).sendToAsync(WorkflowEngineTopic) } returns completed()
}

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

private fun mockTagStateStorage(
  taskIds: Set<TaskId>
): TaskTagStorage {
  val tagStateStorage = mockk<TaskTagStorage>()
  coEvery {
    tagStateStorage.getTaskIdsForTag(
        capture(taskTag),
        capture(serviceName),
    )
  } returns taskIds
  coEvery {
    tagStateStorage.addTaskIdToTag(
        capture(taskTag),
        capture(serviceName),
        capture(taskId),
    )
  } just Runs
  coEvery {
    tagStateStorage.removeTaskIdFromTag(
        capture(taskTag),
        capture(serviceName),
        capture(taskId),
    )
  } just Runs

  return tagStateStorage
}

private fun mockDelegatedTaskDataStorage(delegatedTaskData: DelegatedTaskData? = null): TaskTagStorage {
  val tagStateStorage = mockk<TaskTagStorage>()
  coEvery {
    tagStateStorage.setDelegatedTaskData(
        capture(taskId),
        capture(io.infinitic.services.tag.delegatedTaskData),
    )
  } just Runs
  coEvery { tagStateStorage.delDelegatedTaskData(capture(taskId)) } just Runs
  coEvery { tagStateStorage.getDelegatedTaskData(capture(taskId)) } returns delegatedTaskData

  return tagStateStorage
}

private fun getTagEngine(taskIds: Set<TaskId> = setOf(TaskId())): TaskTagEngine {
  tagStateStorage = mockTagStateStorage(taskIds)
  return TaskTagEngine(tagStateStorage, producerMock)
}

private fun getTaskEngine(delegatedTaskData: DelegatedTaskData? = null): TaskTagEngine {
  tagStateStorage = mockDelegatedTaskDataStorage(delegatedTaskData)
  return TaskTagEngine(tagStateStorage, producerMock)
}

internal class TaskTagEngineTests :
  StringSpec(

      {
        // ensure slots are emptied between each test
        beforeTest {
          taskTag.clear()
          serviceName.clear()
          taskId.clear()
          clientMessage.clear()
          workflowEngineMessage.clear()
          delegatedTaskData.clear()
          clearAllMocks(answers = false)
        }

        "GetTaskIdsByTag should return set of ids" {
          // given
          val msgIn = random<GetTaskIdsByTag>()
          val taskId1 = TaskId()
          val taskId2 = TaskId()
          // when
          getTagEngine(taskIds = setOf(taskId1, taskId2)).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            tagStateStorage.getTaskIdsForTag(msgIn.taskTag, msgIn.serviceName)
            producerMock.producerName
            with(producerMock) { capture(clientMessage).sendToAsync(ClientTopic) }
          }
          captured(taskTag) shouldBe msgIn.taskTag
          captured(serviceName) shouldBe msgIn.serviceName
          captured(clientMessage) shouldBe TaskIdsByTag(
              recipientName = ClientName.from(msgIn.emitterName),
              serviceName = msgIn.serviceName,
              taskTag = msgIn.taskTag,
              taskIds = setOf(taskId1, taskId2),
              emitterName = EmitterName("$workerName"),

              )
        }

        "AddTaskIdToTag should add id" {
          // given
          val msgIn = random<AddTaskIdToTag>()
          // when
          getTagEngine().handle(msgIn, MillisInstant.now())
          // then
          coVerifySequence {
            tagStateStorage.addTaskIdToTag(msgIn.taskTag, msgIn.serviceName, msgIn.taskId)
          }
          captured(taskTag) shouldBe msgIn.taskTag
          captured(serviceName) shouldBe msgIn.serviceName
        }

        "RemoveTaskIdFromTag should remove id" {
          // given
          val msgIn = random<RemoveTaskIdFromTag>()
          // when
          getTagEngine(setOf(msgIn.taskId)).handle(msgIn, MillisInstant.now())
          // then
          coVerifySequence {
            tagStateStorage.removeTaskIdFromTag(msgIn.taskTag, msgIn.serviceName, msgIn.taskId)
          }
          captured(taskTag) shouldBe msgIn.taskTag
          captured(serviceName) shouldBe msgIn.serviceName
          captured(taskId) shouldBe msgIn.taskId
        }

        "SetDelegatedTaskData should set async task data" {
          // given
          val msgIn = random<SetDelegatedTaskData>()
          // when
          getTaskEngine().handle(msgIn, MillisInstant.now())
          // then
          coVerifySequence {
            tagStateStorage.setDelegatedTaskData(msgIn.taskId, msgIn.delegatedTaskData)
          }
          taskId.captured shouldBe msgIn.taskId
          delegatedTaskData.captured shouldBe msgIn.delegatedTaskData
        }

        "CompleteAsyncTask should send RemoteTaskCompleted to parent workflow" {
          // given
          val requester = random<WorkflowRequester>()
          val msgIn = random<CompleteDelegatedTask>()
          val delegatedTaskData = random<DelegatedTaskData>().copy(requester = requester)

          // when
          val emittedAt = MillisInstant.now()
          getTaskEngine(delegatedTaskData).handle(msgIn, emittedAt)
          // then
          coVerifySequence {
            tagStateStorage.getDelegatedTaskData(msgIn.taskId)
            producerMock.producerName
            with(producerMock) { capture(workflowEngineMessage).sendToAsync(WorkflowEngineTopic) }
            tagStateStorage.delDelegatedTaskData(msgIn.taskId)
          }
          taskId.captured shouldBe msgIn.taskId

          workflowEngineMessage.captured shouldBe RemoteTaskCompleted(
              taskReturnValue = TaskReturnValue(
                  taskId = delegatedTaskData.taskId,
                  serviceName = delegatedTaskData.serviceName,
                  methodName = delegatedTaskData.methodName,
                  taskMeta = delegatedTaskData.taskMeta,
                  returnValue = msgIn.returnValue,
              ),
              workflowName = delegatedTaskData.requester.workflowName!!,
              workflowVersion = delegatedTaskData.requester.workflowVersion,
              workflowId = delegatedTaskData.requester.workflowId!!,
              workflowMethodName = delegatedTaskData.requester.workflowMethodName!!,
              workflowMethodId = delegatedTaskData.requester.workflowMethodId!!,
              emitterName = EmitterName("$workerName"),
              emittedAt = emittedAt,
          )
        }

        "CompleteAsyncTask should send RemoteTaskCompleted to parent client" {
          // given
          val requester = random<ClientRequester>()
          val msgIn = random<CompleteDelegatedTask>()
          val delegatedTaskData = random<DelegatedTaskData>().copy(requester = requester)

          // when
          val emittedAt = MillisInstant.now()
          getTaskEngine(delegatedTaskData).handle(msgIn, emittedAt)
          // then
          coVerifySequence {
            tagStateStorage.getDelegatedTaskData(msgIn.taskId)
            producerMock.producerName
            with(producerMock) { capture(clientMessage).sendToAsync(ClientTopic) }
            tagStateStorage.delDelegatedTaskData(msgIn.taskId)
          }
          taskId.captured shouldBe msgIn.taskId

          clientMessage.captured shouldBe TaskCompleted(
              recipientName = delegatedTaskData.requester.clientName!!,
              emitterName = EmitterName("$workerName"),
              taskId = delegatedTaskData.taskId,
              returnValue = msgIn.returnValue,
              taskMeta = delegatedTaskData.taskMeta,
          )
        }

      },
  )
