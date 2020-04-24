package com.zenaton.pulsar.workflows

import com.zenaton.engine.topics.workflows.LoggerInterface
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface
import org.apache.pulsar.functions.api.Context

class Logger(private val context: Context) : LoggerInterface {

    // Json injection
    var json: JsonInterface = Json

    override fun debug(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        context.logger.debug(message)
        return message
    }

    override fun error(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        context.logger.error(message)
        return message
    }

    override fun info(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        context.logger.info(message)
        return message
    }

    override fun warn(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        context.logger.warn(message)
        return message
    }

    override fun trace(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        context.logger.trace(message)
        return message
    }

    private fun getMessage(txt: String, obj1: Any?, obj2: Any?): String {
        val var1 = if (obj1 == null) "" else "\n${json.stringify(obj1, pretty = true)}\n"
        val var2 = if (obj2 == null) "" else "\n${json.stringify(obj2, pretty = true)}\n"

        return String.format(txt, var1, var2)
    }
}
