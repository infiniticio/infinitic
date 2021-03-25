package io.infinitic.common.data

import java.nio.ByteBuffer
import java.util.UUID

fun UUID.asBytes(): ByteArray {
    val b = ByteBuffer.wrap(ByteArray(16))
    b.putLong(mostSignificantBits)
    b.putLong(leastSignificantBits)
    return b.array()
}

fun ByteArray.asUUID(): UUID {
    val bb = ByteBuffer.wrap(this)
    return UUID(bb.long, bb.long)
}
