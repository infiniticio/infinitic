package com.zenaton.engine.logs

import com.zenaton.engine.common.attributes.DecisionId
import com.zenaton.engine.common.attributes.DelayId
import com.zenaton.engine.common.attributes.TaskId
import com.zenaton.engine.common.attributes.WorkflowId

sealed class Message() {

    data class Debug(
        val message: String
    ) : Message()

    data class Info(
        val message: String
    ) : Message()

    data class Warning(
        val message: String
    ) : Message()

    data class Error(
        val message: String
    ) : Message()

    data class Fatal(
        val message: String
    ) : Message()
}

