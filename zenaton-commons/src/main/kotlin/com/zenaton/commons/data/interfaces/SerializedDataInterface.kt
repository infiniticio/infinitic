package com.zenaton.commons.data.interfaces

import com.zenaton.commons.data.AvrosSerializationType
import java.math.BigInteger
import java.security.MessageDigest

interface SerializedDataInterface {
    val serializedData: ByteArray
    val serializationType: AvrosSerializationType

    fun hash(): String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(serializedData)).toString(16).padStart(32, '0')
    }

    fun equalsData(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedDataInterface

        return (serializationType == other.serializationType && serializedData.contentEquals(other.serializedData))
    }

    fun hashCodeData(): Int {
        return serializedData.contentHashCode()
    }
}
