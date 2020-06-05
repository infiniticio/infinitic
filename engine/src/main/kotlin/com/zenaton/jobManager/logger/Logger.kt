package com.zenaton.jobManager.logger

interface Logger {
    fun debug(txt: String, obj1: Any? = null, obj2: Any? = null): String
    fun error(txt: String, obj1: Any? = null, obj2: Any? = null): String
    fun info(txt: String, obj1: Any? = null, obj2: Any? = null): String
    fun warn(txt: String, obj1: Any? = null, obj2: Any? = null): String
    fun trace(txt: String, obj1: Any? = null, obj2: Any? = null): String
}
