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

package io.infinitic.client

import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk

fun mockSendToTagEngine(slots: MutableList<TagEngineMessage>): SendToTagEngine {
    val mock = mockk<SendToTagEngine>()
    coEvery { mock(capture(slots)) } just Runs
    return mock
}

fun mockSendToTaskEngine(
    client: Client,
    message: CapturingSlot<TaskEngineMessage>
): SendToTaskEngine {
    val mock = mockk<SendToTaskEngine>()
    coEvery { mock(capture(message), MillisDuration(0)) } coAnswers {
        val msg = message.captured
        if ((msg is DispatchTask && msg.clientWaiting) || (msg is WaitTask)) {
            client.handle(
                TaskCompleted(
                    clientName = client.clientName,
                    taskId = msg.taskId,
                    taskReturnValue = MethodReturnValue.from("success"),
                    taskMeta = TaskMeta()
                )
            )
        }
    }

    return mock
}

fun mockSendToWorkflowEngine(
    client: Client,
    message: CapturingSlot<WorkflowEngineMessage>
): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    coEvery { mock(capture(message), MillisDuration(0)) } coAnswers {
        val msg = message.captured
        if (msg is DispatchWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
            client.handle(
                WorkflowCompleted(
                    clientName = client.clientName,
                    workflowId = msg.workflowId,
                    workflowReturnValue = MethodReturnValue.from("success")
                )
            )
        }
    }
    return mock
}
