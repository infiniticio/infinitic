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
import io.infinitic.common.clients.messages.TaskIdsPerTag
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.clients.messages.WorkflowIdsPerTag
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.GetTaskIds
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.GetWorkflowIds
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

fun mockSendToTaskTagEngine(
    client: AbstractInfiniticClient,
    taskTagSlots: MutableList<TaskTagEngineMessage>
): SendToTaskTagEngine {
    val sendToTaskTagEngine = mockk<SendToTaskTagEngine>()
    every { sendToTaskTagEngine(capture(taskTagSlots)) } answers {
        client.sendingScope.future {
            taskTagSlots.forEach {
                if (it is GetTaskIds) {
                    val taskIdsPerTag = TaskIdsPerTag(
                        clientName = client.clientName,
                        taskName = it.taskName,
                        taskTag = it.taskTag,
                        taskIds = setOf(TaskId(), TaskId())
                    )
                    delay(100)
                    client.handle(taskIdsPerTag)
                }
            }
        }.join()
    }
    return sendToTaskTagEngine
}

fun mockSendToWorkflowTagEngine(
    client: AbstractInfiniticClient,
    workflowTagSlots: MutableList<WorkflowTagEngineMessage>
): SendToWorkflowTagEngine {
    val sendToWorkflowTagEngine = mockk<SendToWorkflowTagEngine>()
    every { sendToWorkflowTagEngine(capture(workflowTagSlots)) } answers {
        client.sendingScope.future {
            workflowTagSlots.forEach {
                delay(100)
                if (it is GetWorkflowIds) {
                    val workflowIdsPerTag = WorkflowIdsPerTag(
                        clientName = client.clientName,
                        workflowName = it.workflowName,
                        workflowTag = it.workflowTag,
                        workflowIds = setOf(WorkflowId(), WorkflowId())
                    )
                    delay(100)
                    client.handle(workflowIdsPerTag)
                }
            }
        }.join()
    }
    return sendToWorkflowTagEngine
}

fun mockSendToTaskEngine(
    client: AbstractInfiniticClient,
    message: CapturingSlot<TaskEngineMessage>
): SendToTaskEngine {
    val sendToTaskEngine = mockk<SendToTaskEngine>()
    every { sendToTaskEngine(capture(message)) } answers {
        client.sendingScope.future {
            val msg = message.captured
            if ((msg is DispatchTask && msg.clientWaiting) || (msg is WaitTask)) {
                val taskCompleted = TaskCompleted(
                    clientName = client.clientName,
                    taskId = msg.taskId,
                    taskReturnValue = MethodReturnValue.from("success"),
                    taskMeta = TaskMeta()
                )
                delay(100)
                client.handle(taskCompleted)
            }
        }.join()
    }

    return sendToTaskEngine
}

fun mockSendToWorkflowEngine(
    client: AbstractInfiniticClient,
    message: CapturingSlot<WorkflowEngineMessage>
): SendToWorkflowEngine {
    val mock = mockk<SendToWorkflowEngine>()
    every { mock(capture(message)) } answers {
        client.sendingScope.future {
            val msg = message.captured
            if (msg is DispatchWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
                val workflowCompleted = WorkflowCompleted(
                    clientName = client.clientName,
                    workflowId = msg.workflowId,
                    workflowReturnValue = MethodReturnValue.from("success")
                )
                delay(100)
                client.handle(workflowCompleted)
            }
        }.join()
    }

    return mock
}
