package com.zenaton.pulsar.topics.workflows.messages

import com.zenaton.engine.topics.workflows.messages.ChildWorkflowCompleted
import com.zenaton.engine.topics.workflows.messages.DecisionCompleted
import com.zenaton.engine.topics.workflows.messages.DelayCompleted
import com.zenaton.engine.topics.workflows.messages.EventReceived
import com.zenaton.engine.topics.workflows.messages.TaskCompleted
import com.zenaton.engine.topics.workflows.messages.WorkflowCompleted
import com.zenaton.engine.topics.workflows.messages.WorkflowDispatched
import com.zenaton.engine.topics.workflows.messages.WorkflowMessageInterface
import kotlin.reflect.full.declaredMemberProperties

class WorkflowMessageContainer(
    val childWorkflowCompleted: ChildWorkflowCompleted? = null,
    val decisionCompleted: DecisionCompleted? = null,
    val delayCompleted: DelayCompleted? = null,
    val eventReceived: EventReceived? = null,
    val taskCompleted: TaskCompleted? = null,
    val workflowCompleted: WorkflowCompleted? = null,
    val workflowDispatched: WorkflowDispatched? = null
) {
    fun msg(): WorkflowMessageInterface {
        // get list of non null properties
        val msg = WorkflowMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as WorkflowMessageInterface
    }
}
