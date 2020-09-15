package io.infinitic.taskManager.common.data.bases

abstract class Id(open val id: String) : CharSequence by id, Comparable<String> by id {
    final override fun toString() = id
}
