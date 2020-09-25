package io.infinitic.common.taskManager.data.bases

import io.infinitic.common.taskManager.Constants
import java.lang.reflect.Method

abstract class Name(open val name: String) : CharSequence by name, Comparable<String> by name {
    companion object {
        fun fromMethod(method: Method) = "${method.declaringClass.name}${Constants.METHOD_DIVIDER}${method.name}"
    }

    final override fun toString() = name
}
