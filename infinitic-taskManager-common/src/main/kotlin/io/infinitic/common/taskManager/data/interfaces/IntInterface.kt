package io.infinitic.common.data.interfaces

import kotlin.reflect.full.createInstance

interface IntInterface : Comparable<IntInterface> {
    var int: Int

    override operator fun compareTo(other: IntInterface): Int = this.int - other.int
}

operator fun <T : IntInterface> T.plus(increment: Int): T {
    val o = this::class.createInstance()
    o.int = this.int + increment
    return o
}

operator fun <T : IntInterface> T.minus(increment: Int): T {
    val o = this::class.createInstance()
    o.int = this.int - increment
    return o
}

operator fun <T : IntInterface> T.inc(): T {
    this.int++
    return this
}

operator fun <T : IntInterface> T.dec(): T {
    this.int--
    return this
}
