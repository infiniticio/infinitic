package com.zenaton.pulsar.topics.tasks.messages

import com.zenaton.engine.tasks.interfaces.TaskMessageInterface
import com.zenaton.engine.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.tasks.messages.TaskDispatched
import kotlin.reflect.full.declaredMemberProperties

class TaskMessageContainer {
    var taskAttemptCompleted: TaskAttemptCompleted? = null
    var taskAttemptFailed: TaskAttemptFailed? = null
    var taskAttemptStarted: TaskAttemptStarted? = null
    var taskDispatched: TaskDispatched? = null

    constructor(msg: TaskMessageInterface) {
        when (msg) {
            is TaskAttemptCompleted -> this.taskAttemptCompleted = msg
            is TaskAttemptFailed -> this.taskAttemptFailed = msg
            is TaskAttemptStarted -> this.taskAttemptStarted = msg
            is TaskDispatched -> taskDispatched = msg
        }
    }

    fun msg(): TaskMessageInterface {
        // get list of non null properties
        val msg = TaskMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as TaskMessageInterface
    }
}
