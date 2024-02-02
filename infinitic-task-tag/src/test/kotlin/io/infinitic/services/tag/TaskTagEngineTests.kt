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

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.AddTaskIdToTag
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTaskIdFromTag
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.workers.data.WorkerName
import io.infinitic.tasks.tag.TaskTagEngine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture

private fun <T : Any> captured(slot: CapturingSlot<T>) =
    if (slot.isCaptured) slot.captured else null

private val workername = WorkerName("clientTaskTagEngineTests")

private var stateMessageId = slot<String>()
private var stateTaskId = slot<String>()
private var clientSlot = slot<ClientMessage>()
private var taskExecutorSlot = slot<ServiceExecutorMessage>()
private var delaySlot = slot<MillisDuration>()


private lateinit var tagStateStorage: TaskTagStorage

private fun completed() = CompletableFuture.completedFuture(Unit)

val producerMock = mockk<InfiniticProducerAsync> {
  every { producerName } returns "$workername"
  coEvery { capture(clientSlot).sendToAsync(ClientTopic) } returns completed()
  coEvery {
    capture(taskExecutorSlot).sendToAsync(ServiceExecutorTopic, capture(delaySlot))
  } returns completed()
}

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

private fun mockTagStateStorage(
  tag: TaskTag,
  name: ServiceName,
  taskIds: Set<TaskId>
): TaskTagStorage {
  val tagStateStorage = mockk<TaskTagStorage>()
  coEvery { tagStateStorage.getTaskIdsForTag(tag, name) } returns taskIds
  coEvery { tagStateStorage.addTaskIdToTag(tag, name, TaskId(capture(stateTaskId))) } just Runs
  coEvery { tagStateStorage.removeTaskIdFromTag(tag, name, TaskId(capture(stateTaskId))) } just Runs

  return tagStateStorage
}

private fun getEngine(
  taskTag: TaskTag,
  serviceName: ServiceName,
  taskIds: Set<TaskId> = setOf(TaskId())
): TaskTagEngine {
  tagStateStorage = mockTagStateStorage(taskTag, serviceName, taskIds)

  return TaskTagEngine(tagStateStorage, producerMock)
}

internal class TaskTagEngineTests :
  StringSpec(

      {
        // ensure slots are emptied between each test
        beforeTest {
          stateMessageId.clear()
          clientSlot.clear()
          taskExecutorSlot.clear()
          delaySlot.clear()
        }

        "addTaskTag should add id" {
          // given
          val msgIn = random<AddTaskIdToTag>()
          // when
          getEngine(msgIn.taskTag, msgIn.serviceName).handle(msgIn, MillisInstant.now())
          // then
          coVerifySequence {
            tagStateStorage.addTaskIdToTag(msgIn.taskTag, msgIn.serviceName, msgIn.taskId)
          }
        }

        "removeTaskTag should remove id" {
          // given
          val msgIn = random<RemoveTaskIdFromTag>()
          // when
          getEngine(msgIn.taskTag, msgIn.serviceName, taskIds = setOf(msgIn.taskId)).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            tagStateStorage.removeTaskIdFromTag(msgIn.taskTag, msgIn.serviceName, msgIn.taskId)
          }
        }

        "getTaskIdsPerTag should return set of ids" {
          // given
          val msgIn = random<GetTaskIdsByTag>()
          val taskId1 = TaskId()
          val taskId2 = TaskId()
          // when
          getEngine(msgIn.taskTag, msgIn.serviceName, taskIds = setOf(taskId1, taskId2)).handle(
              msgIn,
              MillisInstant.now(),
          )
          // then
          coVerifySequence {
            tagStateStorage.getTaskIdsForTag(msgIn.taskTag, msgIn.serviceName)
            producerMock.producerName
            with(producerMock) { ofType<TaskIdsByTag>().sendToAsync(ClientTopic) }
          }

          captured(clientSlot).shouldBeInstanceOf<TaskIdsByTag>()
          (captured(clientSlot) as TaskIdsByTag).taskIds shouldBe setOf(taskId1, taskId2)
        }
      },
  )
