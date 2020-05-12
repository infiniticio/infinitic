package com.zenaton.engine.topics.tasks.interfaces

import com.zenaton.engine.data.TaskAttemptId

interface TaskAttemptMessageInterface : TaskMessageInterface {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
