package com.zenaton.engine.workflows

import com.zenaton.engine.workflows.messages.WorkflowMessage

interface LoggerInterface {
    fun debug(txt: String, msg: WorkflowMessage? = null): String
    fun error(txt: String, msg: WorkflowMessage? = null): String
    fun info(txt: String, msg: WorkflowMessage? = null): String
    fun warn(txt: String, msg: WorkflowMessage? = null): String
    fun trace(txt: String, msg: WorkflowMessage? = null): String
}
