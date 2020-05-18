package com.zenaton.taskmanager.pulsar.logger

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.logger.TaskLoggerInterface
import com.zenaton.taskmanager.pulsar.Topic
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger

class TaskLogger(val context: Context) : TaskLoggerInterface {
    // Json injection
    private var json = Json
    // Logger injection
    private var logger: Logger = context.logger

    override fun debug(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.debug(message)
        dispatch(context, message, "DEBUG")
        return message
    }

    override fun error(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.error(message)
        dispatch(context, message, "ERROR")
        return message
    }

    override fun info(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.info(message)
        dispatch(context, message, "INFO")
        return message
    }

    override fun warn(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.warn(message)
        dispatch(context, message, "WARN")
        return message
    }

    override fun trace(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.trace(message)
        dispatch(context, message, "TRACE")
        return message
    }

    private fun getMessage(txt: String, obj1: Any?, obj2: Any?): String {
        val var1 = if (obj1 == null) "" else "\n${json.stringify(obj1, pretty = true)}\n"
        val var2 = if (obj2 == null) "" else "\n${json.stringify(obj2, pretty = true)}\n"

        return String.format(txt, var1, var2)
    }

    private fun dispatch(context: Context, msg: String, level: String): MessageId {
        return context
            .newOutputMessage(Topic.LOGS.get(), Schema.STRING)
            .value("$level - $msg")
            .send()
    }
}
