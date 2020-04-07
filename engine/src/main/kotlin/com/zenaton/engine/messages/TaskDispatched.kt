package com.zenaton.engine.messages

import java.io.Serializable

class TaskDispatched(
    var taskId: String,
    var taskName: String
) : Serializable
