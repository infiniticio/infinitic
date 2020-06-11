package com.zenaton.workflowManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.DecisionData
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.DecisionOutput
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventData
import com.zenaton.workflowManager.data.EventName
import com.zenaton.workflowManager.data.TaskData
import com.zenaton.workflowManager.data.TaskId
import com.zenaton.workflowManager.data.TaskOutput
import com.zenaton.workflowManager.data.WorkflowData
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.WorkflowName
import com.zenaton.workflowManager.data.WorkflowOutput
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

sealed class Message

data class CancelWorkflow(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val workflowOutput: WorkflowOutput?
) : Message(), ForWorkflowEngineMessage

data class ChildWorkflowCanceled(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: WorkflowOutput?
) : Message(), ForWorkflowEngineMessage

data class ChildWorkflowCompleted(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: WorkflowOutput?
) : Message(), ForWorkflowEngineMessage

data class DecisionCompleted(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val decisionId: DecisionId,
    val decisionOutput: DecisionOutput
) : Message(), ForWorkflowEngineMessage

data class DecisionDispatched(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val decisionId: DecisionId,
    val workflowName: WorkflowName,
    val decisionData: DecisionData
) : Message(), ForWorkflowEngineMessage

data class DelayCompleted(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val delayId: DelayId
) : Message(), ForWorkflowEngineMessage

data class DispatchWorkflow(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val workflowName: WorkflowName,
    val workflowData: WorkflowData
) : Message(), ForWorkflowEngineMessage

data class EventReceived(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val eventName: EventName,
    val eventData: EventData?
) : Message(), ForWorkflowEngineMessage

data class TaskCanceled(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : Message(), ForWorkflowEngineMessage

data class TaskCompleted(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : Message(), ForWorkflowEngineMessage

data class TaskDispatched(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val taskId: TaskId,
    val taskData: TaskData?
) : Message(), ForWorkflowEngineMessage

data class WorkflowCanceled(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val workflowOutput: TaskOutput?
) : Message(), ForWorkflowEngineMessage

data class WorkflowCompleted(
    override val workflowId: WorkflowId,
    override val sentAt: DateTime = DateTime(),
    val workflowOutput: TaskOutput?
) : Message(), ForWorkflowEngineMessage
