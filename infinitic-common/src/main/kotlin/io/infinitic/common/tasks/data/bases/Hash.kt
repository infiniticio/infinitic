package io.infinitic.common.tasks.data.bases

abstract class Hash(open val hash: String) : CharSequence by hash, Comparable<String> by hash {
    final override fun toString() = hash
}
