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

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.SendToChannelCompleted
import io.infinitic.common.clients.messages.SendToChannelFailed
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tags.messages.SendToChannelPerTag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk

internal class MockClientOutput(
    tagSlot: CapturingSlot<TagEngineMessage>,
    taskSlot: CapturingSlot<TaskEngineMessage>,
    workflowSlot: CapturingSlot<WorkflowEngineMessage>
) : ClientOutput {
    override val clientName = ClientName("clientTest")
    override val sendToTaskEngineFn = mockk<SendToTaskEngine>()
    override val sendToWorkflowEngineFn = mockk<SendToWorkflowEngine>()
    override val sendToTagEngineFn = mockk<SendToTagEngine>()
    lateinit var client: Client

    init {
        coEvery { sendToTagEngineFn(capture(tagSlot)) } coAnswers {
            val msg = tagSlot.captured
            if (msg is SendToChannelPerTag && msg.clientWaiting) {
                client.handle(
                    if (msg.channelEvent.get() == "unknown") {
                        SendToChannelFailed(
                            clientName = clientName,
                            channelEventId = msg.channelEventId
                        )
                    } else {
                        SendToChannelCompleted(
                            clientName = clientName,
                            channelEventId = msg.channelEventId
                        )
                    }

                )
            }
        }
        coEvery { sendToTaskEngineFn(capture(taskSlot), MillisDuration(0)) } coAnswers {
            val msg = taskSlot.captured
            if (msg is DispatchTask && msg.clientWaiting) {
                client.handle(
                    TaskCompleted(
                        clientName = clientName,
                        taskId = msg.taskId,
                        taskReturnValue = MethodReturnValue.from("success")
                    )
                )
            }
        }
        coEvery { sendToWorkflowEngineFn(capture(workflowSlot), MillisDuration(0)) } coAnswers {
            val msg = workflowSlot.captured
            if (msg is DispatchWorkflow && msg.clientWaiting) {
                client.handle(
                    WorkflowCompleted(
                        clientName = clientName,
                        workflowId = msg.workflowId,
                        workflowReturnValue = MethodReturnValue.from("success")
                    )
                )
            }
        }
    }
}
