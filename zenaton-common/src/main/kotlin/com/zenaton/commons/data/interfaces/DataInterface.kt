package com.zenaton.common.data.interfaces

import java.math.BigInteger
import java.security.MessageDigest

interface DataInterface {
    val data: ByteArray

    fun hash(): String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(data)).toString(16).padStart(32, '0')
    }

    fun equalsData(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataInterface

        if (!data.contentEquals(other.data)) return false

        return true
    }

    fun hashCodeData(): Int {
        return data.contentHashCode()
    }
}
