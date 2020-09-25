package io.infinitic.common.taskManager.data.bases

abstract class Hash(open val hash: String) : CharSequence by hash, Comparable<String> by hash {
    final override fun toString() = hash
}
