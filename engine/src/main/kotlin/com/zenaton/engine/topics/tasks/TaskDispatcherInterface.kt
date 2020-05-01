package com.zenaton.engine.topics.tasks

import com.zenaton.engine.topics.tasks.messages.TaskAttemptDispatched

interface TaskDispatcherInterface {
    fun dispatchTaskAttempt(msg: TaskAttemptDispatched)
}
