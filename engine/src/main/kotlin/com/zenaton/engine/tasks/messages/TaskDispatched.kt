package com.zenaton.engine.tasks.messages

import java.io.Serializable

class TaskDispatched(
    var taskId: String,
    var taskName: String
) : Serializable
