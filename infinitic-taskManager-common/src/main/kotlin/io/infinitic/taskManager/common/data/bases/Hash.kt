package io.infinitic.taskManager.common.data.bases

abstract class Hash(open val hash: String) : CharSequence by hash, Comparable<String> by hash {
    final override fun toString() = hash
}
