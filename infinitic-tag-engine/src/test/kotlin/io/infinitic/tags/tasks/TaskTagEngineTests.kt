/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tags.tasks

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MessageId
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.tasks.tags.messages.CancelTaskByTag
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RemoveTagFromTask
import io.infinitic.common.tasks.tags.messages.RetryTaskByTag
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.tags.tasks.storage.TaskTagStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

private fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

private val clientName = ClientName("clientTaskTagEngineTests")

private lateinit var stateMessageId: CapturingSlot<String>
private lateinit var stateTaskId: CapturingSlot<String>
private lateinit var clientMessage: CapturingSlot<ClientMessage>
private lateinit var taskEngineMessage: CapturingSlot<TaskEngineMessage>

private lateinit var tagStateStorage: TaskTagStorage
private lateinit var sendToTaskEngine: SendToTaskEngine
private lateinit var sendToClient: SendToClient

internal class TaskTagEngineTests : StringSpec({

    "should not handle known messageId" {
        // given
        val msgIn = random<TaskTagEngineMessage>()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName, msgIn.messageId).handle(msgIn)
        // then
        verifyAll()
    }

    "should store last messageId" {
        // given
        val msgIn = random<TaskTagEngineMessage>()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName).handle(msgIn)
        // then
        coVerify {
            tagStateStorage.setLastMessageId(msgIn.taskTag, msgIn.taskName, msgIn.messageId)
        }
    }

    "addTaskTag should complete id" {
        // given
        val msgIn = random<AddTagToTask>()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.addTaskId(msgIn.taskTag, msgIn.taskName, msgIn.taskId)
            tagStateStorage.setLastMessageId(msgIn.taskTag, msgIn.taskName, msgIn.messageId)
        }
        verifyAll()
    }

    "removeTaskTag should remove id" {
        // given
        val msgIn = random<RemoveTagFromTask>()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName, taskIds = setOf(msgIn.taskId)).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.removeTaskId(msgIn.taskTag, msgIn.taskName, msgIn.taskId)
            tagStateStorage.setLastMessageId(msgIn.taskTag, msgIn.taskName, msgIn.messageId)
        }
        verifyAll()
    }

    "retryTaskPerTag should retry task" {
        // given
        val taskIds = setOf(TaskId(), TaskId())
        val msgIn = random<RetryTaskByTag>()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName, taskIds = taskIds).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getLastMessageId(msgIn.taskTag, msgIn.taskName)
            tagStateStorage.getTaskIds(msgIn.taskTag, msgIn.taskName)
            sendToTaskEngine(ofType<RetryTask>())
            sendToTaskEngine(ofType<RetryTask>())
            tagStateStorage.setLastMessageId(msgIn.taskTag, msgIn.taskName, msgIn.messageId)
        }
        verifyAll()
        // checking last message
        val retryTask = captured(taskEngineMessage)!! as RetryTask

        with(retryTask) {
            taskId shouldBe taskIds.last()
        }
    }

    "cancelTaskPerTag should cancel task" {
        // given
        val taskIds = setOf(TaskId(), TaskId())
        val msgIn = random<CancelTaskByTag>()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName, taskIds = taskIds).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getLastMessageId(msgIn.taskTag, msgIn.taskName)
            tagStateStorage.getTaskIds(msgIn.taskTag, msgIn.taskName)
            sendToTaskEngine(ofType<CancelTask>())
            sendToTaskEngine(ofType<CancelTask>())
            tagStateStorage.setLastMessageId(msgIn.taskTag, msgIn.taskName, msgIn.messageId)
        }
        verifyAll()
        // checking last message
        val cancelTask = captured(taskEngineMessage)!! as CancelTask

        with(cancelTask) {
            taskId shouldBe taskIds.last()
        }
    }

    "getTaskIdsPerTag should return set of ids" {
        // given
        val msgIn = random<GetTaskIdsByTag>()
        val taskId1 = TaskId()
        val taskId2 = TaskId()
        // when
        getEngine(msgIn.taskTag, msgIn.taskName, taskIds = setOf(taskId1, taskId2)).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getTaskIds(msgIn.taskTag, msgIn.taskName)
            sendToClient(ofType<TaskIdsByTag>())
            tagStateStorage.setLastMessageId(msgIn.taskTag, msgIn.taskName, msgIn.messageId)
        }
        verifyAll()

        captured(clientMessage).shouldBeInstanceOf<TaskIdsByTag>()
        (captured(clientMessage) as TaskIdsByTag).taskIds shouldBe setOf(taskId1, taskId2)
    }
})

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

private fun mockSendToClient(slot: CapturingSlot<ClientMessage>): SendToClient {
    val mock = mockk<SendToClient>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

private fun mockSendToTaskEngine(slots: CapturingSlot<TaskEngineMessage>): SendToTaskEngine {
    val mock = mockk<SendToTaskEngine>()
    coEvery { mock(capture(slots)) } just Runs
    return mock
}

private fun mockTagStateStorage(tag: TaskTag, name: TaskName, messageId: MessageId?, taskIds: Set<TaskId>): TaskTagStorage {
    val tagStateStorage = mockk<TaskTagStorage>()
    coEvery { tagStateStorage.getLastMessageId(tag, name) } returns messageId
    coEvery { tagStateStorage.setLastMessageId(tag, name, MessageId(capture(stateMessageId))) } just Runs
    coEvery { tagStateStorage.getTaskIds(tag, name) } returns taskIds
    coEvery { tagStateStorage.addTaskId(tag, name, TaskId(capture(stateTaskId))) } just Runs
    coEvery { tagStateStorage.removeTaskId(tag, name, TaskId(capture(stateTaskId))) } just Runs

    return tagStateStorage
}

private fun getEngine(
    taskTag: TaskTag,
    taskName: TaskName,
    messageId: MessageId? = MessageId(),
    taskIds: Set<TaskId> = setOf(TaskId())
): TaskTagEngine {
    stateMessageId = slot()
    stateTaskId = slot()
    clientMessage = slot()
    taskEngineMessage = slot()

    tagStateStorage = mockTagStateStorage(taskTag, taskName, messageId, taskIds)
    sendToTaskEngine = mockSendToTaskEngine(taskEngineMessage)
    sendToClient = mockSendToClient(clientMessage)

    return TaskTagEngine(clientName, tagStateStorage, sendToTaskEngine, sendToClient)
}

private fun verifyAll() = confirmVerified(
    sendToTaskEngine
)
