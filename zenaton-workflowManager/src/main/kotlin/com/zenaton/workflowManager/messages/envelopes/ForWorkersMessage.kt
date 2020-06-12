package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.TaskId

interface ForWorkersMessage {
    val taskId: TaskId
    val sentAt: DateTime
}
