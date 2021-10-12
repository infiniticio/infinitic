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

package io.infinitic.tags.workflows

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MessageId
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
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

private val clientName = ClientName("clientWorkflowTagEngineTests")

private lateinit var stateMessageId: CapturingSlot<String>
private lateinit var stateWorkflowId: CapturingSlot<String>
private lateinit var workflowEngineMessage: CapturingSlot<WorkflowEngineMessage>
private lateinit var clientMessage: CapturingSlot<ClientMessage>

private lateinit var workflowTagStorage: WorkflowTagStorage
private lateinit var sendToWorkflowEngine: SendToWorkflowEngine
private lateinit var sendToClient: SendToClient

internal class WorkflowTagEngineTests : StringSpec({

    "should not handle known messageId" {
        // given
        val msgIn = random<WorkflowTagEngineMessage>()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName, msgIn.messageId).handle(msgIn)
        // then
        verifyAll()
    }

    "should store last messageId" {
        // given
        val msgIn = random<WorkflowTagEngineMessage>()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName).handle(msgIn)
        // then
        coVerify {
            workflowTagStorage.setLastMessageId(msgIn.workflowTag, msgIn.workflowName, msgIn.messageId)
        }
    }

    "cancelWorkflowPerTag should cancel workflow" {
        // given
        val workflowIds = setOf(WorkflowId(), WorkflowId())
        val msgIn = random<CancelWorkflowByTag>()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(msgIn)
        // then
        coVerifySequence {
            workflowTagStorage.getLastMessageId(msgIn.workflowTag, msgIn.workflowName)
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            sendToWorkflowEngine(ofType<CancelWorkflow>())
            sendToWorkflowEngine(ofType<CancelWorkflow>())
            workflowTagStorage.setLastMessageId(msgIn.workflowTag, msgIn.workflowName, msgIn.messageId)
        }
        verifyAll()
        // checking last message
        val cancelWorkflow = captured(workflowEngineMessage)!! as CancelWorkflow

        with(cancelWorkflow) {
            workflowId shouldBe workflowIds.last()
        }
    }

    "sendToChannelPerTag should send to channel" {
        // given
        val workflowIds = setOf(WorkflowId(), WorkflowId())
        val msgIn = random<SendSignalByTag>()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(msgIn)
        // then
        coVerifySequence {
            workflowTagStorage.getLastMessageId(msgIn.workflowTag, msgIn.workflowName)
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            sendToWorkflowEngine(ofType<SendSignal>())
            sendToWorkflowEngine(ofType<SendSignal>())
            workflowTagStorage.setLastMessageId(msgIn.workflowTag, msgIn.workflowName, msgIn.messageId)
        }
        verifyAll()
        // checking last message
        val sendSignal = captured(workflowEngineMessage)!! as SendSignal

        with(sendSignal) {
            workflowId shouldBe workflowIds.last()
            workflowName shouldBe msgIn.workflowName
            channelSignal shouldBe msgIn.channelSignal
            channelSignalId shouldBe msgIn.channelSignalId
            channelSignal shouldBe msgIn.channelSignal
            channelSignalTypes shouldBe msgIn.channelSignalTypes
            channelName shouldBe msgIn.channelName
        }
    }

    "getWorkflowIdsPerTag should return set of ids" {
        // given
        val msgIn = random<GetWorkflowIdsByTag>()
        val workflowId1 = WorkflowId()
        val workflowId2 = WorkflowId()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = setOf(workflowId1, workflowId2)).handle(msgIn)
        // then
        coVerifySequence {
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            sendToClient(ofType<WorkflowIdsByTag>())
            workflowTagStorage.setLastMessageId(msgIn.workflowTag, msgIn.workflowName, msgIn.messageId)
        }
        verifyAll()

        captured(clientMessage).shouldBeInstanceOf<WorkflowIdsByTag>()
        (captured(clientMessage) as WorkflowIdsByTag).workflowIds shouldBe setOf(workflowId1, workflowId2)
    }
})

private inline fun <reified T : Any> random(values: Map<String, Any?>? = null) =
    TestFactory.random<T>(values)

private fun mockSendToClient(slot: CapturingSlot<ClientMessage>): SendToClient {
    val mock = mockk<SendToClient>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

private fun mockSendToWorkflowEngine(slot: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    coEvery { mock(capture(slot)) } just Runs
    return mock
}

private fun mockWorkflowTagStorage(
    workflowTag: WorkflowTag,
    workflowName: WorkflowName,
    messageId: MessageId?,
    workflowIds: Set<WorkflowId>
): WorkflowTagStorage {
    val tagStateStorage = mockk<WorkflowTagStorage>()
    coEvery { tagStateStorage.getLastMessageId(workflowTag, workflowName) } returns messageId
    coEvery { tagStateStorage.setLastMessageId(workflowTag, workflowName, MessageId(capture(stateMessageId))) } just Runs
    coEvery { tagStateStorage.getWorkflowIds(workflowTag, workflowName) } returns workflowIds
    coEvery { tagStateStorage.addWorkflowId(workflowTag, workflowName, WorkflowId(capture(stateWorkflowId))) } just Runs
    coEvery { tagStateStorage.removeWorkflowId(workflowTag, workflowName, WorkflowId(capture(stateWorkflowId))) } just Runs

    return tagStateStorage
}

private fun getEngine(
    workflowTag: WorkflowTag,
    workflowName: WorkflowName,
    messageId: MessageId? = MessageId(),
    workflowIds: Set<WorkflowId> = setOf(WorkflowId())
): WorkflowTagEngine {
    stateMessageId = slot()
    stateWorkflowId = slot()
    clientMessage = slot()
    workflowEngineMessage = slot()

    workflowTagStorage = mockWorkflowTagStorage(workflowTag, workflowName, messageId, workflowIds)
    sendToWorkflowEngine = mockSendToWorkflowEngine(workflowEngineMessage)
    sendToClient = mockSendToClient(clientMessage)

    return WorkflowTagEngine(clientName, workflowTagStorage, sendToWorkflowEngine, sendToClient)
}

private fun verifyAll() = confirmVerified(
    sendToWorkflowEngine
)
