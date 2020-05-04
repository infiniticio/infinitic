package com.zenaton.pulsar.utils

import com.zenaton.engine.interfaces.LoggerInterface
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger

class Logger(private val context: Context) :
    LoggerInterface {

    // Json injection
    var json: JsonInterface = Json
    private val logger: Logger = context.logger

    override fun debug(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.debug(message)
        return message
    }

    override fun error(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.error(message)
        return message
    }

    override fun info(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.info(message)
        return message
    }

    override fun warn(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.warn(message)
        return message
    }

    override fun trace(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.trace(message)
        return message
    }

    private fun getMessage(txt: String, obj1: Any?, obj2: Any?): String {
        val var1 = if (obj1 == null) "" else "\n${json.stringify(obj1, pretty = true)}\n"
        val var2 = if (obj2 == null) "" else "\n${json.stringify(obj2, pretty = true)}\n"

        return String.format(txt, var1, var2)
    }
}
