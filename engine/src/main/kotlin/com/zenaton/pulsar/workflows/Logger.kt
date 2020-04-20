package com.zenaton.pulsar.workflows

import com.zenaton.engine.workflows.LoggerInterface
import com.zenaton.engine.workflows.WorkflowMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface
import org.apache.pulsar.functions.api.Context

class Logger(private val context: Context) : LoggerInterface {

    // Json injection
    var json: JsonInterface = Json

    override fun debug(txt: String, msg: WorkflowMessage?): String {
        val message = getMessage(txt, msg)
        context.logger.debug(message)
        return message
    }

    override fun error(txt: String, msg: WorkflowMessage?): String {
        val message = getMessage(txt, msg)
        context.logger.error(message)
        return message
    }

    override fun info(txt: String, msg: WorkflowMessage?): String {
        val message = getMessage(txt, msg)
        context.logger.info(message)
        return message
    }

    override fun warn(txt: String, msg: WorkflowMessage?): String {
        val message = getMessage(txt, msg)
        context.logger.warn(message)
        return message
    }

    override fun trace(txt: String, msg: WorkflowMessage?): String {
        val message = getMessage(txt, msg)
        context.logger.trace(message)
        return message
    }

    private fun getMessage(txt: String, msg: WorkflowMessage?): String {
        return txt + msg?.let { json.to(it) }
    }
}
