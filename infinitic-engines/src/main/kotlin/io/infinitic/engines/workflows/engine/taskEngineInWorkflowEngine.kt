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

package io.infinitic.engines.workflows.engine

import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.common.SendToWorkflowEngine
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.infinitic.engines.tasks.engine.TaskEngine
import io.infinitic.engines.tasks.storage.TaskStateStorage
import io.infinitic.common.workflows.messages.TaskCompleted as TaskCompletedInWorkflow

fun taskEngineInWorkflowEngine(
    storage: TaskStateStorage,
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskEngine: SendToTaskEngine,
    sendToMonitoringPerName: SendToMonitoringPerName,
    sendToWorkers: SendToWorkers
) = TaskEngine(
    storage,
    { message: TaskEngineMessage, delay: Float ->
        // normal behavior
        sendToTaskEngine(message, delay)
        // if a TaskCompleted is send to task engine, we send it also to workflow engine
        when (message) {
            is TaskCompleted -> {
                val wid = message.taskMeta.get(WorkflowEngine.META_WORKFLOW_ID)
                wid?.let {
                    val workflowId = WorkflowId("$wid")
                    sendToWorkflowEngine(
                        when ("${message.taskName}") {
                            WorkflowTask::class.java.name -> WorkflowTaskCompleted(
                                workflowId = workflowId,
                                workflowTaskId = WorkflowTaskId("${message.taskId}"),
                                workflowTaskOutput = message.taskOutput.get() as WorkflowTaskOutput
                            )
                            else -> TaskCompletedInWorkflow(
                                workflowId = workflowId,
                                methodRunId = MethodRunId(message.taskMeta.get(WorkflowEngine.META_METHOD_RUN_ID).toString()),
                                taskId = message.taskId,
                                taskOutput = message.taskOutput
                            )
                        },
                        0F
                    )
                }
            }
            else -> Unit
        }
    },
    sendToMonitoringPerName,
    sendToWorkers
)
