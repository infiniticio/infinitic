package io.infinitic.common.tasks.data.bases

import io.infinitic.common.tasks.Constants
import java.lang.reflect.Method

abstract class Name(open val name: String) : CharSequence by name, Comparable<String> by name {
    companion object {
        fun fromMethod(method: Method) = "${method.declaringClass.name}${Constants.METHOD_DIVIDER}${method.name}"
    }

    final override fun toString() = name
}
