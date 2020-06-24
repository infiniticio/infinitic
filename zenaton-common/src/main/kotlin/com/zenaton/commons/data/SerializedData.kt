package com.zenaton.common.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.common.avro.AvroSerDe
import com.zenaton.common.json.Json
import org.apache.avro.specific.SpecificRecord
import java.math.BigInteger
import java.security.MessageDigest

open class SerializedData(
    open val serializedData: ByteArray,
    open val serializationType: SerializationType
) {
    constructor() : this(
        serializedData = ByteArray(0),
        serializationType = SerializationType.NULL
    )

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

    // type checking
    @JsonIgnore fun isNull() = (serializationType == SerializationType.NULL)
    @JsonIgnore fun isBytes() = (serializationType == SerializationType.BYTES)
    @JsonIgnore fun isJson() = (serializationType == SerializationType.JSON)
    @JsonIgnore fun isAvro() = (serializationType == SerializationType.AVRO)

    // retrieve bytes
    fun fromBytes() = if (isBytes()) serializedData else
        throw Exception("Trying to retrieve bytes from a $serializationType type")

    // retrieve from json
    inline fun <reified T : Any>fromJson() = if(isJson()) Json.parse<T>(String(serializedData, Charsets.UTF_8)) else
        throw Exception("Trying to retrieve value using json from a $serializationType type")

    // retrieve from avro
    inline fun <reified T : SpecificRecord>fromAvro() = if(isAvro()) AvroSerDe.deserializeFromByteArray<T>(serializedData) else
        throw Exception("Trying to retrieve value using avro from a $serializationType type")
}
