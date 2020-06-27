package com.zenaton.common.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.common.avro.AvroSerDe
import com.zenaton.common.json.Json
import org.apache.avro.specific.SpecificRecord
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest

data class SerializedData(
    var serializedData: ByteArray,
    var serializationType: SerializationType
) {
    companion object  {
        fun from(value: Any? = null) : SerializedData {
            val data: ByteArray
            val type: SerializationType

            when(value) {
                null -> {
                    data = ByteArray(0)
                    type = SerializationType.NULL
                }
                is ByteArray -> {
                    data = value
                    type = SerializationType.BYTES
                }
                is ByteBuffer -> {
                    val p = value.position()
                    val bytes = ByteArray(value.remaining())
                    value.get(bytes, 0, bytes.size)
                    value.position(p)

                    data = bytes
                    type = SerializationType.BYTES
                }
                is SpecificRecord -> {
                    data = AvroSerDe.serializeToByteArray(value)
                    type = SerializationType.AVRO
                }
                else -> {
                    data = Json.stringify(value).toByteArray(Charsets.UTF_8)
                    type = SerializationType.JSON
                }
            }

            return SerializedData(data, type)
        }
    }

    fun hash(): String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(serializedData)).toString(16).padStart(32, '0')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedData

        return (serializationType == other.serializationType && serializedData.contentEquals(other.serializedData))
    }

    override fun hashCode(): Int {
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
    inline fun <reified T : Any> fromJson() = if (isJson()) Json.parse<T>(String(serializedData, Charsets.UTF_8)) else
        throw Exception("Trying to retrieve value using json from a $serializationType type")

    // retrieve from avro
    inline fun <reified T : SpecificRecord> fromAvro() = if (isAvro()) AvroSerDe.deserializeFromByteArray<T>(serializedData) else
        throw Exception("Trying to retrieve value using avro from a $serializationType type")
}
