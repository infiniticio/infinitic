package com.zenaton.engine.messages

import java.io.Serializable

class WorkflowDispatched(
    var workflowId: String,
    var workflowName: String
) : Serializable
