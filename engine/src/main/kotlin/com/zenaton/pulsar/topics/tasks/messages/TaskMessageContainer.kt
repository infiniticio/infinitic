package com.zenaton.pulsar.topics.tasks.messages

import com.zenaton.engine.topics.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptDispatched
import com.zenaton.engine.topics.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.topics.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.engine.topics.tasks.messages.TaskMessageInterface
import com.zenaton.engine.topics.workflows.messages.TaskCompleted
import kotlin.reflect.full.declaredMemberProperties

class TaskMessageContainer(
    val taskAttemptCompleted: TaskAttemptCompleted? = null,
    val taskAttemptDispatched: TaskAttemptDispatched? = null,
    val taskAttemptFailed: TaskAttemptFailed? = null,
    val taskAttemptStarted: TaskAttemptStarted? = null,
    val taskCompleted: TaskCompleted? = null,
    val taskDispatched: TaskDispatched? = null
) {
    fun msg(): TaskMessageInterface {
        // get list of non null properties
        val msg = TaskMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as TaskMessageInterface
    }
}
