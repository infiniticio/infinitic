// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engine.workflowManager.engines

import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.common.workflows.messages.TaskCompleted as ForWorkflowTaskCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.messaging.api.dispatcher.Emetter
import io.infinitic.messaging.api.dispatcher.Receiver

class ForWorkflowTaskEngine(
    protected override val storage: TaskStateStorage,
    protected override val receiver: Receiver<ForTaskEngineMessage>,
    val workflowEngineEmetter: Emetter<ForWorkflowEngineMessage>,
    protected override val taskEngineEmetter: Emetter<ForTaskEngineMessage>,
    protected override val monitoringPerNameEmetter: Emetter<ForMonitoringPerNameMessage>,
    protected override val workerEmetter: Emetter<ForWorkerMessage>
) : TaskEngine(storage, receiver, taskEngineEmetter, monitoringPerNameEmetter, workerEmetter) {
    override suspend fun handle(message: ForTaskEngineMessage) {
        super.handle(message)

        // dispatch Task and WorkflowTask to workflow engine after completion
        if (message is TaskCompleted) {
            val wid = message.taskMeta[WorkflowEngine.META_WORKFLOW_ID]
            if (wid != null) {
                val workflowId = WorkflowId("$wid")
                val msg = when ("${message.taskName}") {
                    WorkflowTask::class.java.name -> WorkflowTaskCompleted(
                        workflowId = workflowId,
                        workflowTaskId = WorkflowTaskId("${message.taskId}"),
                        workflowTaskOutput = message.taskOutput.data as WorkflowTaskOutput
                    )
                    else -> ForWorkflowTaskCompleted(
                        workflowId = workflowId,
                        methodRunId = MethodRunId(message.taskMeta[WorkflowEngine.META_METHOD_RUN_ID].toString()),
                        taskId = message.taskId,
                        taskOutput = message.taskOutput
                    )
                }
                workflowEngineEmetter.send(msg)
            }
        }
    }
}
