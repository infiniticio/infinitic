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

package io.infinitic.tags.engine

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.Name
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.AddTaskTag
import io.infinitic.common.tags.messages.CancelTaskPerTag
import io.infinitic.common.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.tags.messages.RemoveTaskTag
import io.infinitic.common.tags.messages.RetryTaskPerTag
import io.infinitic.common.tags.messages.SendToChannelPerTag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tags.engine.storage.TagStateStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID

fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

lateinit var tagStateMessageId: CapturingSlot<MessageId>
lateinit var tagStateUUID: CapturingSlot<UUID>
lateinit var clientMessage: CapturingSlot<ClientMessage>
lateinit var taskEngineMessage: CapturingSlot<TaskEngineMessage>
lateinit var workflowEngineMessage: CapturingSlot<WorkflowEngineMessage>

lateinit var tagStateStorage: TagStateStorage
lateinit var sendToClient: SendToClient
lateinit var sendToTaskEngine: SendToTaskEngine
lateinit var sendToWorkflowEngine: SendToWorkflowEngine

class TagEngineTests : StringSpec({

    "should not handle known messageId" {
        // given
        val msgIn = random<TagEngineMessage>()
        // when
        getEngine(msgIn.tag, msgIn.name, msgIn.messageId).handle(msgIn)
        // then
        verifyAll()
    }

    "should store last messageId" {
        // given
        val msgIn = random<TagEngineMessage>()
        // when
        getEngine(msgIn.tag, msgIn.name).handle(msgIn)
        // then
        coVerify {
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
    }

    "addTaskTag should complete id" {
        // given
        val msgIn = random<AddTaskTag>()
        // when
        getEngine(msgIn.tag, msgIn.name).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.addId(msgIn.tag, msgIn.name, msgIn.taskId.id)
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
        verifyAll()
    }

    "removeTaskTag should remove id" {
        // given
        val msgIn = random<RemoveTaskTag>()
        // when
        getEngine(msgIn.tag, msgIn.name, ids = setOf(msgIn.taskId.id)).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.removeId(msgIn.tag, msgIn.name, msgIn.taskId.id)
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
        verifyAll()
    }

    "retryTaskPerTag should retry task" {
        // given
        val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
        val msgIn = random<RetryTaskPerTag>()
        // when
        getEngine(msgIn.tag, msgIn.name, ids = ids).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getLastMessageId(msgIn.tag, msgIn.name)
            tagStateStorage.getIds(msgIn.tag, msgIn.name)
            sendToTaskEngine(ofType<RetryTask>(), MillisDuration(0))
            sendToTaskEngine(ofType<RetryTask>(), MillisDuration(0))
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
        verifyAll()
        val retryTask = captured(taskEngineMessage)!! as RetryTask

        with(retryTask) {
            taskId.id shouldBe ids.last()
        }
    }

    "cancelTaskPerTag should cancel task" {
        // given
        val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
        val msgIn = random<CancelTaskPerTag>()
        // when
        getEngine(msgIn.tag, msgIn.name, ids = ids).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getLastMessageId(msgIn.tag, msgIn.name)
            tagStateStorage.getIds(msgIn.tag, msgIn.name)
            sendToTaskEngine(ofType<CancelTask>(), MillisDuration(0))
            sendToTaskEngine(ofType<CancelTask>(), MillisDuration(0))
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
        verifyAll()
        val cancelTask = captured(taskEngineMessage)!! as CancelTask

        with(cancelTask) {
            taskId.id shouldBe ids.last()
        }
    }

    "cancelWorkflowPerTag should cancel workflow" {
        // given
        val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
        val msgIn = random<CancelWorkflowPerTag>()
        // when
        getEngine(msgIn.tag, msgIn.name, ids = ids).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getLastMessageId(msgIn.tag, msgIn.name)
            tagStateStorage.getIds(msgIn.tag, msgIn.name)
            sendToWorkflowEngine(ofType<CancelWorkflow>(), MillisDuration(0))
            sendToWorkflowEngine(ofType<CancelWorkflow>(), MillisDuration(0))
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
        verifyAll()
        val cancelWorkflow = captured(workflowEngineMessage)!! as CancelWorkflow

        with(cancelWorkflow) {
            workflowId.id shouldBe ids.last()
        }
    }

    "sendToChannelPerTag should send to channel" {
        // given
        val ids = setOf(UUID.randomUUID(), UUID.randomUUID())
        val msgIn = random<SendToChannelPerTag>()
        // when
        getEngine(msgIn.tag, msgIn.name, ids = ids).handle(msgIn)
        // then
        coVerifySequence {
            tagStateStorage.getLastMessageId(msgIn.tag, msgIn.name)
            tagStateStorage.getIds(msgIn.tag, msgIn.name)
            sendToWorkflowEngine(ofType<SendToChannel>(), MillisDuration(0))
            sendToWorkflowEngine(ofType<SendToChannel>(), MillisDuration(0))
            tagStateStorage.setLastMessageId(msgIn.tag, msgIn.name, msgIn.messageId)
        }
        verifyAll()
        val sendToChannel = captured(workflowEngineMessage)!! as SendToChannel

        with(sendToChannel) {
            workflowId.id shouldBe ids.last()
            workflowName shouldBe msgIn.name
            channelEvent shouldBe msgIn.channelEvent
            channelEventId shouldBe msgIn.channelEventId
            channelEvent shouldBe msgIn.channelEvent
            channelEventTypes shouldBe msgIn.channelEventTypes
            channelName shouldBe msgIn.channelName
        }
    }
})

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

fun mockSendToClient(slot: CapturingSlot<ClientMessage>): SendToClient {
    val mock = mockk<SendToClient>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

fun mockSendToTaskEngine(slots: CapturingSlot<TaskEngineMessage>): SendToTaskEngine {
    val mock = mockk<SendToTaskEngine>()
    coEvery { mock(capture(slots), MillisDuration(0)) } just Runs
    return mock
}

fun mockSendToWorkflowEngine(slot: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    coEvery { mock(capture(slot), MillisDuration(0)) } just Runs
    return mock
}

fun mockTagStateStorage(tag: Tag, name: Name, messageId: MessageId?, ids: Set<UUID>): TagStateStorage {
    val tagStateStorage = mockk<TagStateStorage>()
    coEvery { tagStateStorage.getLastMessageId(tag, name) } returns messageId
    coEvery { tagStateStorage.setLastMessageId(tag, name, capture(tagStateMessageId)) } just Runs
    coEvery { tagStateStorage.getIds(tag, name) } returns ids
    coEvery { tagStateStorage.addId(tag, name, capture(tagStateUUID)) } just Runs
    coEvery { tagStateStorage.removeId(tag, name, capture(tagStateUUID)) } just Runs

    return tagStateStorage
}

fun getEngine(tag: Tag, name: Name, messageId: MessageId? = MessageId(), ids: Set<UUID> = setOf(UUID.randomUUID())): TagEngine {
    tagStateMessageId = slot()
    tagStateUUID = slot()
    clientMessage = slot()
    taskEngineMessage = slot()
    workflowEngineMessage = slot()

    tagStateStorage = mockTagStateStorage(tag, name, messageId, ids)
    sendToClient = mockSendToClient(clientMessage)
    sendToTaskEngine = mockSendToTaskEngine(taskEngineMessage)
    sendToWorkflowEngine = mockSendToWorkflowEngine(workflowEngineMessage)

    return TagEngine(
        tagStateStorage,
        sendToClient,
        sendToTaskEngine,
        sendToWorkflowEngine
    )
}

fun verifyAll() = confirmVerified(
    sendToClient,
    sendToTaskEngine,
    sendToWorkflowEngine
)
