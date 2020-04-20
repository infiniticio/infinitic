package com.zenaton.engine.common.attributes

import java.math.BigInteger
import java.security.MessageDigest

abstract class Data(open val data: ByteArray) {
    fun hash() : String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(data)).toString(16).padStart(32, '0')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskData

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
