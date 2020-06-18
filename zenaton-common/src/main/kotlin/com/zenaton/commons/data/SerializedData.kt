package com.zenaton.common.data

import java.math.BigInteger
import java.security.MessageDigest

open class SerializedData(
    open val serializedData: ByteArray,
    open val serializationType: AvrosSerializationType
) {
    fun hash(): String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(serializedData)).toString(16).padStart(32, '0')
    }

    // final is important here for inheritance by data class
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedData

        return (serializationType == other.serializationType && serializedData.contentEquals(other.serializedData))
    }

    // final is important here for inheritance by data class
    final override fun hashCode(): Int {
        return serializedData.contentHashCode()
    }
}
