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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.TaskIdsByTag
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import java.util.concurrent.CopyOnWriteArrayList

fun mockSendToTaskTagEngine(
    client: InfiniticClient,
    taskTagSlots: CopyOnWriteArrayList<TaskTagEngineMessage>,
    clientName: ClientName,
    sendingScope: CoroutineScope
): SendToTaskTagEngine {
    val sendToTaskTagEngine = mockk<SendToTaskTagEngine>()
    every { sendToTaskTagEngine(capture(taskTagSlots)) } answers {
        taskTagSlots.forEach {
            if (it is GetTaskIdsByTag) {
                val taskIdsByTag = TaskIdsByTag(
                    recipientName = clientName,
                    taskName = it.taskName,
                    taskTag = it.taskTag,
                    taskIds = setOf(TaskId(), TaskId()),
                    emitterName = ClientName("mockk")
                )
                sendingScope.future {
                    delay(100)
                    client.handle(taskIdsByTag)
                }
            }
        }
    }
    return sendToTaskTagEngine
}

fun mockSendToWorkflowTagEngine(
    client: InfiniticClient,
    workflowTagSlots: CopyOnWriteArrayList<WorkflowTagEngineMessage>,
    clientName: ClientName,
    sendingScope: CoroutineScope
): SendToWorkflowTagEngine {
    val sendToWorkflowTagEngine = mockk<SendToWorkflowTagEngine>()
    every { sendToWorkflowTagEngine(capture(workflowTagSlots)) } answers {
        workflowTagSlots.forEach {
            if (it is GetWorkflowIdsByTag) {
                val workflowIdsByTag = WorkflowIdsByTag(
                    recipientName = clientName,
                    workflowName = it.workflowName,
                    workflowTag = it.workflowTag,
                    workflowIds = setOf(WorkflowId(), WorkflowId()),
                    emitterName = ClientName("mockk")
                )
                sendingScope.future {
                    delay(100)
                    client.handle(workflowIdsByTag)
                }
            }
        }
    }
    return sendToWorkflowTagEngine
}

fun mockSendToTaskEngine(
    client: InfiniticClient,
    message: CapturingSlot<TaskEngineMessage>,
    clientName: ClientName,
    sendingScope: CoroutineScope
): SendToTaskEngine {
    val sendToTaskEngine = mockk<SendToTaskEngine>()
    every { sendToTaskEngine(capture(message)) } answers {
        val msg = message.captured
        if ((msg is DispatchTask && msg.clientWaiting) || (msg is WaitTask)) {
            val taskCompleted = TaskCompleted(
                recipientName = clientName,
                emitterName = ClientName("mockk"),
                taskId = msg.taskId,
                taskReturnValue = ReturnValue.from("success"),
                taskMeta = TaskMeta()
            )
            sendingScope.future {
                delay(100)
                client.handle(taskCompleted)
            }
        }
    }

    return sendToTaskEngine
}

fun mockSendToWorkflowEngine(
    client: InfiniticClient,
    message: CapturingSlot<WorkflowEngineMessage>,
    clientName: ClientName,
    sendingScope: CoroutineScope
): SendToWorkflowEngine {
    val sendToWorkflowEngine = mockk<SendToWorkflowEngine>()
    every { sendToWorkflowEngine(capture(message)) } answers {
        val msg: WorkflowEngineMessage = message.captured
        if (msg is DispatchWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
            val workflowCompleted = MethodCompleted(
                recipientName = clientName,
                workflowId = msg.workflowId,
                methodRunId = MethodRunId.from(msg.workflowId),
                methodReturnValue = ReturnValue.from("success"),
                emitterName = ClientName("mockk")
            )
            sendingScope.future {
                delay(100)
                client.handle(workflowCompleted)
            }
        }
    }

    return sendToWorkflowEngine
}
