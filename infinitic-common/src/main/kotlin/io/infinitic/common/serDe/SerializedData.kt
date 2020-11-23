/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.serDe

import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.serDe.kotlin.getKSerializerOrNull
import io.infinitic.common.tasks.exceptions.ClassNotFoundDuringDeserialization
import io.infinitic.common.tasks.exceptions.ExceptionDuringJsonDeserialization
import io.infinitic.common.tasks.exceptions.ExceptionDuringKotlinDeserialization
import io.infinitic.common.tasks.exceptions.MissingMetaJavaClassDuringDeserialization
import io.infinitic.common.tasks.exceptions.SerializerNotFoundDuringDeserialization
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.apache.avro.specific.SpecificRecordBase
import java.math.BigInteger
import java.security.MessageDigest
import io.infinitic.common.json.Json as JsonJackson
import kotlinx.serialization.json.Json as JsonKotlin

@Serializable
data class SerializedData(
    var bytes: ByteArray,
    var type: SerializedDataType,
    val meta: Map<String, ByteArray> = mapOf()
) {
    companion object {
        // meta key containing the name of the serialized java class
        const val META_JAVA_CLASS = "javaClass"

        /**
         * @return serialized value
         */
        fun <T : Any> from(value: T?): SerializedData {
            val bytes: ByteArray
            val type: SerializedDataType
            val meta = mapOf(META_JAVA_CLASS to (value ?: "")::class.java.name.toByteArray(charset = Charsets.UTF_8))

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
                    bytes = toAvroJavaByteArray(value)
                    type = SerializedDataType.AVRO_JAVA
                }
                else -> {
                    val serializer = getKSerializerOrNull(value::class.java)
                    if (serializer == null) {
                        bytes = toJsonJacksonByteArray(value)
                        type = SerializedDataType.JSON_JACKSON
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        bytes = toJsonKotlinByteArray(serializer as KSerializer<T>, value)
                        type = SerializedDataType.JSON_KOTLIN
                    }
                }
            }
            return SerializedData(bytes, type, meta)
        }

        private fun toJsonJacksonByteArray(value: Any): ByteArray =
            JsonJackson.stringify(value).toByteArray(charset = Charsets.UTF_8)

        private fun <T : Any> toJsonKotlinByteArray(serializer: KSerializer<T>, value: T): ByteArray =
            JsonKotlin.encodeToString(serializer, value).toByteArray(charset = Charsets.UTF_8)

        private fun toAvroJavaByteArray(value: SpecificRecordBase): ByteArray =
            AvroSerDe.serializeToByteArray(value)
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
            throw ClassNotFoundDuringDeserialization(this, klassName)
        }

        return deserialize(klass)
    }

    fun hash(): String {
        // MD5 implementation, enough to avoid collision in practical cases
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
    }

    override fun toString() = try { "${deserialize()}" } catch (e: Throwable) {
        "** SerializedData - can't display due to deserialization error :**\n$e"
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

    private fun deserialize(klass: Class<*>) = when (type) {
        SerializedDataType.NULL -> null
        SerializedDataType.BYTES -> bytes
        SerializedDataType.JSON_JACKSON -> fromJsonJackson(klass)
        SerializedDataType.JSON_KOTLIN -> fromJsonKotlin(klass)
        SerializedDataType.AVRO_JAVA -> fromAvroJava(klass)
        SerializedDataType.CUSTOM -> throw RuntimeException("Can't deserialize data with CUSTOM serialization")
    }

    private fun <T : Any> fromJsonJackson(klass: Class<out T>): T = try {
        JsonJackson.parse(String(bytes, Charsets.UTF_8), klass)
    } catch (e: JsonProcessingException) {
        throw ExceptionDuringJsonDeserialization(klass.name, causeString = e.toString())
    }

    private fun fromJsonKotlin(klass: Class<*>): Any? {
        val serializer = getKSerializerOrNull(klass) ?: throw SerializerNotFoundDuringDeserialization(klass.name)

        return try {
            JsonKotlin.decodeFromString(serializer, String(bytes, Charsets.UTF_8))
        } catch (e: SerializationException) {
            throw ExceptionDuringKotlinDeserialization(klass.name, causeString = e.toString())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> fromAvroJava(klass: Class<out T>): SpecificRecordBase {
        return AvroSerDe.deserializeFromByteArray(bytes, klass as Class<out SpecificRecordBase>)
    }
}
