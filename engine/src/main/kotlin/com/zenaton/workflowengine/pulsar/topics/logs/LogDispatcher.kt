package com.zenaton.workflowengine.pulsar.topics.logs

import com.zenaton.workflowengine.pulsar.topics.Topic
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides methods to send a text into the logs topic
 */
object LogDispatcher {
    fun debug(context: Context, msg: String) = dispatch(context, msg, "DEBUG")

    fun error(context: Context, msg: String) = dispatch(context, msg, "ERROR")

    fun info(context: Context, msg: String) = dispatch(context, msg, "INFO")

    fun warn(context: Context, msg: String) = dispatch(context, msg, "WARN")

    fun trace(context: Context, msg: String) = dispatch(context, msg, "TRACE")

    private fun dispatch(context: Context, msg: String, level: String): MessageId {
        return context
            .newOutputMessage(Topic.LOGS.get(), Schema.STRING)
            .value("$level - $msg")
            .send()
    }
}
