package com.zenaton.workflowManager.pulsar.topics.workflows.messages

import com.zenaton.workflowManager.topics.workflows.interfaces.WorkflowMessageInterface
import com.zenaton.workflowManager.messages.ChildWorkflowCompleted
import com.zenaton.workflowManager.messages.DecisionCompleted
import com.zenaton.workflowManager.messages.EventReceived
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.WorkflowCompleted
import com.zenaton.workflowManager.messages.WorkflowDispatched
import com.zenaton.workflowManager.topics.delays.messages.DelayCompleted
import kotlin.reflect.full.declaredMemberProperties

class WorkflowMessageContainer {
    var childWorkflowCompleted: ChildWorkflowCompleted? = null
    var decisionCompleted: DecisionCompleted? = null
    var delayCompleted: DelayCompleted? = null
    var eventReceived: EventReceived? = null
    var taskCompleted: TaskCompleted? = null
    var workflowCompleted: WorkflowCompleted? = null
    var workflowDispatched: WorkflowDispatched? = null

    constructor(msg: WorkflowMessageInterface) {
        when (msg) {
            is ChildWorkflowCompleted -> this.childWorkflowCompleted = msg
            is DecisionCompleted -> this.decisionCompleted = msg
            is DelayCompleted -> this.delayCompleted = msg
            is EventReceived -> this.eventReceived = msg
            is TaskCompleted -> this.taskCompleted = msg
            is WorkflowCompleted -> this.workflowCompleted = msg
            is WorkflowDispatched -> this.workflowDispatched = msg
        }
    }

    fun msg(): WorkflowMessageInterface {
        // get list of non null properties
        val msg = WorkflowMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as WorkflowMessageInterface
    }
}
