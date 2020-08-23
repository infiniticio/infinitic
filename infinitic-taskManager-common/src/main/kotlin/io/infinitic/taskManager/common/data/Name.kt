package io.infinitic.taskManager.common.data

import io.infinitic.taskManager.common.Constants
import java.lang.reflect.Method

abstract class Name(open val name: String) {
    companion object {
        fun fromMethod(method: Method) = "${method.declaringClass.name}${Constants.METHOD_DIVIDER}${method.name}"
    }

    override fun toString() = name
}
