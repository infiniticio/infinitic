package com.zenaton.engine.data.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigInteger
import java.security.MessageDigest

abstract class Data @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@get:JsonValue open val data: ByteArray) {

    open fun hash(): String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(data)).toString(16).padStart(32, '0')
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data

        if (!data.contentEquals(other.data)) return false

        return true
    }

    final override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
