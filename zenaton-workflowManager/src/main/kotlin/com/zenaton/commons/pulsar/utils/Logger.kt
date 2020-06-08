package com.zenaton.commons.pulsar.utils

import com.zenaton.commons.utils.json.Json
import com.zenaton.workflowengine.interfaces.LoggerInterface
import com.zenaton.workflowengine.pulsar.topics.logs.LogDispatcher
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger

class Logger(val context: Context) : LoggerInterface {
    // Json injection
    private var json = Json
    // Logger injection
    private var logger: Logger = context.logger
    // LogDispatcher injection
    private var logDispatcher = LogDispatcher

    override fun debug(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.debug(message)
        logDispatcher.debug(context, message)
        return message
    }

    override fun error(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.error(message)
        logDispatcher.error(context, message)
        return message
    }

    override fun info(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.info(message)
        logDispatcher.info(context, message)
        return message
    }

    override fun warn(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.warn(message)
        logDispatcher.warn(context, message)
        return message
    }

    override fun trace(txt: String, obj1: Any?, obj2: Any?): String {
        val message = getMessage(txt, obj1, obj2)
        logger.trace(message)
        logDispatcher.trace(context, message)
        return message
    }

    private fun getMessage(txt: String, obj1: Any?, obj2: Any?): String {
        val var1 = if (obj1 == null) "" else "\n${json.stringify(obj1, pretty = true)}\n"
        val var2 = if (obj2 == null) "" else "\n${json.stringify(obj2, pretty = true)}\n"

        return String.format(txt, var1, var2)
    }
}
