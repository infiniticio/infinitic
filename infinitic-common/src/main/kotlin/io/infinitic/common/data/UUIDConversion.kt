package io.infinitic.common.data

import java.nio.ByteBuffer
import java.util.UUID

object UUIDConversion {
    fun UUID.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.wrap(ByteArray(16))
        byteBuffer.putLong(this.mostSignificantBits)
        byteBuffer.putLong(this.leastSignificantBits)
        return byteBuffer.array()
    }
    fun ByteArray.toUUID(): UUID {
        val byteBuffer = ByteBuffer.wrap(this)
        val mostSignificantBits = byteBuffer.long
        val leastSignificantBits = byteBuffer.long
        return UUID(mostSignificantBits, leastSignificantBits)
    }
}
