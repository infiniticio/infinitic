// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.data

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.json.Json
import io.infinitic.common.tasks.exceptions.MissingMetaJavaClassDuringDeserialization
import io.infinitic.common.tasks.exceptions.UnknownReturnClassDuringDeserialization
import kotlinx.serialization.Serializable
import org.apache.avro.specific.SpecificRecordBase
import java.math.BigInteger
import java.security.MessageDigest

@Serializable
data class SerializedData(
    var bytes: ByteArray,
    var type: SerializedDataType,
//    val meta: Map<String, ByteArray> = mapOf()
    val meta: Map<String, String> = mapOf()
) {
    companion object {
        // meta key containing the name of the serialized java class
        const val META_JAVA_CLASS = "javaClass"

        /**
         * @return serialized value
         */
        fun from(value: Any?): SerializedData {
            val bytes: ByteArray
            val type: SerializedDataType
//            val meta = mapOf(META_JAVA_CLASS to (value ?: "")::class.java.name.toByteArray(charset = Charsets.UTF_8))
            val meta = mapOf(META_JAVA_CLASS to (value ?: "")::class.java.name)

            when (value) {
                null -> {
                    bytes = ByteArray(0)
                    type = SerializedDataType.NULL
                }
                is ByteArray -> {
                    bytes = value
                    type = SerializedDataType.BYTES
                }
                is SpecificRecordBase -> {
                    bytes = AvroSerDe.serializeToByteArray(value)
                    type = SerializedDataType.AVRO
                }
                else -> {
                    bytes = Json.stringify(value).toByteArray(charset = Charsets.UTF_8)
                    type = SerializedDataType.JSON
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
        SerializedDataType.NULL -> null
        SerializedDataType.BYTES -> bytes
        SerializedDataType.JSON -> fromJson(klass)
        SerializedDataType.AVRO -> fromAvro(klass)
        SerializedDataType.CUSTOM -> throw RuntimeException("Can't deserialize data with CUSTOM serialization")
    }

    /**
     * @return deserialized value
     */
    fun deserialize(): Any? {
//        val klassName = meta[META_JAVA_CLASS]?.let { String(it, charset = Charsets.UTF_8) }
        val klassName = meta[META_JAVA_CLASS]?.let { it }
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

    override fun toString() = try { "${deserialize()}" } catch (e: Throwable) {
        "** error during deserialization:**\n$e"
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

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> fromAvro(klass: Class<out T>) = AvroSerDe.deserializeFromByteArray(bytes, klass as Class<out SpecificRecordBase>)
}

enum class SerializedDataType {
    NULL,
    BYTES,
    JSON,
    AVRO,
    CUSTOM
}
