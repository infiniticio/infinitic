package io.infinitic.common.tasks.data.bases

abstract class Id(open val id: String) : CharSequence by id, Comparable<String> by id {
    final override fun toString() = id
}
