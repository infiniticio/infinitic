package com.zenaton.common.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.common.avro.AvroSerDe
import com.zenaton.common.json.Json
import com.zenaton.jobManager.data.AvroSerializedDataType
import org.apache.avro.specific.SpecificRecord
import org.apache.avro.specific.SpecificRecordBase
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest

data class SerializedData(
    var bytes: ByteArray,
    var type: AvroSerializedDataType,
    val meta: Map<String, ByteArray> = mapOf()
) {
    companion object {
        fun from(value: Any?, meta: Map<String, ByteArray> = mapOf()): SerializedData {
            val bytes: ByteArray
            val type: AvroSerializedDataType

            when (value) {
                null -> {
                    bytes = ByteArray(0)
                    type = AvroSerializedDataType.NULL
                }
                is ByteArray -> {
                    bytes = value
                    type = AvroSerializedDataType.BYTES
                }
                is ByteBuffer -> {
                    bytes = value.array()
                    type = AvroSerializedDataType.BYTES
                }
                is SpecificRecordBase -> {
                    bytes = AvroSerDe.serializeToByteArray(value)
                    type = AvroSerializedDataType.AVRO
                }
                else -> {
                    bytes = Json.stringify(value).toByteArray(charset = Charsets.UTF_8)
                    type = AvroSerializedDataType.JSON
                }
            }
            return SerializedData(bytes, type, meta)
        }
    }

    fun hash(): String {
        // MD5 implementation
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedData

        return (type == other.type && bytes.contentEquals(other.bytes))
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    /**
     * @return true if value is null, else false
     */
    @JsonIgnore fun isNull() = (type == AvroSerializedDataType.NULL)

    /**
     * @return true if deserialize value is a bytes array, else false
     */
    @JsonIgnore fun isByteArray() = (type == AvroSerializedDataType.BYTES)

    /**
     * @return true if json-encoded, else false
     */
    @JsonIgnore fun isJson() = (type == AvroSerializedDataType.JSON)

    /**
     * @return true if avro-encoded, else false
     */
    @JsonIgnore fun isAvro() = (type == AvroSerializedDataType.AVRO)

    /**
     * @return true if custom-encoded, else false
     */
    @JsonIgnore fun isCustom() = (type == AvroSerializedDataType.CUSTOM)

    /**
     * @return deserialize value
     *
     * @throws Exception if the type is not BYTES
     */
    fun fromBytes() = if (isByteArray()) bytes else
        throw Exception("Trying to retrieve bytes from a $type type")

    /**
     * @return deserialize value
     *
     * @throws Exception if type is not JSON
     */
    fun <T : Any> fromJson(klass: Class<out T>): T = if (isJson()) Json.parse(String(bytes, Charsets.UTF_8), klass) else
        throw Exception("Trying to retrieve value using json from a $type type")

    /**
     * @return deserialize value
     *
     * @throws Exception if type is not JSON
     */
    inline fun <reified T : Any> fromJson(): T = fromJson(T::class.javaObjectType)

    /**
     * @return deserialize value
     *
     * @throws Exception if type is not AVRO
     */
    fun <T : SpecificRecord> fromAvro(klass: Class<out T>) = if (isAvro()) AvroSerDe.deserializeFromByteArray(bytes, klass) else
        throw Exception("Trying to retrieve value using avro from a $type type")

    /**
     * @return deserialize value
     *
     * @throws Exception if type is not AVRO
     */
    inline fun <reified T : SpecificRecord> fromAvro() = fromAvro(T::class.javaObjectType)
}
