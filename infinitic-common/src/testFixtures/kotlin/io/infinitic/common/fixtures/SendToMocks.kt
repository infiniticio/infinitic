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

package io.infinitic.common.fixtures

import io.infinitic.common.clients.SendToClient
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.executors.SendToTaskExecutor
import io.infinitic.common.tasks.executors.SendToTaskExecutorAfter
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList

fun mockSendToClient(message: CapturingSlot<ClientMessage>): SendToClient {
    val sendToClient = mockk<SendToClient>()
    coEvery { sendToClient(capture(message)) } just Runs

    return sendToClient
}

fun mockSendToTaskTag(message: CopyOnWriteArrayList<TaskTagMessage>): SendToTaskTag {
    val sendToTaskTag = mockk<SendToTaskTag>()
    coEvery { sendToTaskTag(capture(message)) } just Runs

    return sendToTaskTag
}

fun mockSendToTaskExecutor(
    message: CapturingSlot<TaskExecutorMessage>
): SendToTaskExecutor {
    val sendToTaskExecutor = mockk<SendToTaskExecutor>()
    coEvery { sendToTaskExecutor(capture(message)) } just Runs

    return sendToTaskExecutor
}

fun mockSendToTaskExecutorAfter(
    message: CapturingSlot<TaskExecutorMessage>,
    delay: CapturingSlot<MillisDuration>
): SendToTaskExecutorAfter {
    val sendToTaskExecutorAfter = mockk<SendToTaskExecutorAfter>()
    coEvery { sendToTaskExecutorAfter(capture(message), capture(delay)) } just Runs

    return sendToTaskExecutorAfter
}

fun mockSendToWorkflowTaskExecutor(
    message: CapturingSlot<TaskExecutorMessage>
): SendToTaskExecutor {
    val sendToTaskExecutor = mockk<SendToTaskExecutor>()
    coEvery { sendToTaskExecutor(capture(message)) } just Runs

    return sendToTaskExecutor
}

fun mockSendToWorkflowTag(message: CopyOnWriteArrayList<WorkflowTagMessage>): SendToWorkflowTag {
    val sendToWorkflowTag = mockk<SendToWorkflowTag>()
    coEvery { sendToWorkflowTag(capture(message)) } just Runs

    return sendToWorkflowTag
}

fun mockSendToWorkflowEngine(message: CapturingSlot<WorkflowEngineMessage>): SendToWorkflowEngine {
    val sendToWorkflowEngine = mockk<SendToWorkflowEngine>()
    coEvery { sendToWorkflowEngine(capture(message)) } just Runs

    return sendToWorkflowEngine
}

fun mockSendToWorkflowEngineAfter(
    message: CapturingSlot<WorkflowEngineMessage>,
    delay: CapturingSlot<MillisDuration>
): SendToWorkflowEngineAfter {
    val sendToWorkflowEngineAfter = mockk<SendToWorkflowEngineAfter>()
    coEvery { sendToWorkflowEngineAfter(capture(message), capture(delay)) } just Runs

    return sendToWorkflowEngineAfter
}
