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

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
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

private fun <T : Any> captured(slot: CapturingSlot<T>) = if (slot.isCaptured) slot.captured else null

private lateinit var stateMessageId: CapturingSlot<MessageId>
private lateinit var stateWorkflowId: CapturingSlot<WorkflowId>
private lateinit var workflowEngineMessage: CapturingSlot<WorkflowEngineMessage>

private lateinit var workflowTagStorage: WorkflowTagStorage
private lateinit var sendToWorkflowEngine: SendToWorkflowEngine

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
        val msgIn = random<CancelWorkflowPerTag>()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(msgIn)
        // then
        coVerifySequence {
            workflowTagStorage.getLastMessageId(msgIn.workflowTag, msgIn.workflowName)
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            sendToWorkflowEngine(ofType<CancelWorkflow>(), MillisDuration(0))
            sendToWorkflowEngine(ofType<CancelWorkflow>(), MillisDuration(0))
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
        val msgIn = random<SendToChannelPerTag>()
        // when
        getEngine(msgIn.workflowTag, msgIn.workflowName, workflowIds = workflowIds).handle(msgIn)
        // then
        coVerifySequence {
            workflowTagStorage.getLastMessageId(msgIn.workflowTag, msgIn.workflowName)
            workflowTagStorage.getWorkflowIds(msgIn.workflowTag, msgIn.workflowName)
            sendToWorkflowEngine(ofType<SendToChannel>(), MillisDuration(0))
            sendToWorkflowEngine(ofType<SendToChannel>(), MillisDuration(0))
            workflowTagStorage.setLastMessageId(msgIn.workflowTag, msgIn.workflowName, msgIn.messageId)
        }
        verifyAll()
        // checking last message
        val sendToChannel = captured(workflowEngineMessage)!! as SendToChannel

        with(sendToChannel) {
            workflowId shouldBe workflowIds.last()
            workflowName shouldBe msgIn.workflowName
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

private fun mockSendToWorkflowEngine(slot: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    coEvery { mock(capture(slot), MillisDuration(0)) } just Runs
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
    coEvery { tagStateStorage.setLastMessageId(workflowTag, workflowName, capture(stateMessageId)) } just Runs
    coEvery { tagStateStorage.getWorkflowIds(workflowTag, workflowName) } returns workflowIds
    coEvery { tagStateStorage.addWorkflowId(workflowTag, workflowName, capture(stateWorkflowId)) } just Runs
    coEvery { tagStateStorage.removeWorkflowId(workflowTag, workflowName, capture(stateWorkflowId)) } just Runs

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
    workflowEngineMessage = slot()

    workflowTagStorage = mockWorkflowTagStorage(workflowTag, workflowName, messageId, workflowIds)
    sendToWorkflowEngine = mockSendToWorkflowEngine(workflowEngineMessage)

    return WorkflowTagEngine(workflowTagStorage, sendToWorkflowEngine)
}

private fun verifyAll() = confirmVerified(
    sendToWorkflowEngine
)
