package io.infinitic.common.data

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.exceptions.MissingMetaJavaClassDuringDeserialization
import io.infinitic.taskManager.common.exceptions.UnknownReturnClassDuringDeserialization
import io.infinitic.taskManager.data.AvroSerializedDataType
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
        // meta key containing the name of the serialized java class
        const val META_JAVA_CLASS = "javaClass"

        /**
         * @return serialized value
         */
        fun from(value: Any?): SerializedData {
            val bytes: ByteArray
            val type: AvroSerializedDataType
            val meta = mapOf(META_JAVA_CLASS to (value ?: "")::class.java.name.toByteArray(charset = Charsets.UTF_8))

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

    /**
     * @return deserialized value
     * @param
     */
    fun deserialize(klass: Class<*>) = when (type) {
        AvroSerializedDataType.NULL -> null
        AvroSerializedDataType.BYTES -> bytes
        AvroSerializedDataType.JSON -> fromJson(klass)
        AvroSerializedDataType.AVRO -> fromAvro(klass)
        else -> throw Exception("Can't deserialize data with CUSTOM serialization")
    }

    /**
     * @return deserialized value
     */
    fun deserialize(): Any? {
        val klassName = meta[META_JAVA_CLASS]?.let { String(it, charset = Charsets.UTF_8) }
        if (klassName === null) throw MissingMetaJavaClassDuringDeserialization(this)

        val klass = try {
            Class.forName(klassName)
        } catch (e: ClassNotFoundException) {
            throw UnknownReturnClassDuringDeserialization(this, klassName)
        }

        return deserialize(klass)
    }

    fun hash(): String {
        // MD5 implementation, enough to avoid collision in practical cases
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedData

        if (!bytes.contentEquals(other.bytes)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    private fun <T : Any> fromJson(klass: Class<out T>): T = Json.parse(String(bytes, Charsets.UTF_8), klass)

    private fun <T : Any> fromAvro(klass: Class<out T>) = AvroSerDe.deserializeFromByteArray(bytes, klass as Class<out SpecificRecordBase>)
}
