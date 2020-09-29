package io.infinitic.engine.workflowManager.engines

import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskOutput
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.messages.TaskCompleted as ForWorkflowTaskCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.infinitic.messaging.api.dispatcher.Dispatcher

class ForWorkflowTaskEngine(
    override val storage: TaskStateStorage,
    override val dispatcher: Dispatcher
) : TaskEngine(storage, dispatcher) {
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
                dispatcher.toWorkflowEngine(msg)
            }
        }
    }
}
