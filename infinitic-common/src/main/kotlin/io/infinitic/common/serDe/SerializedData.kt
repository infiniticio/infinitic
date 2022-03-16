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
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.exceptions.serialization.ClassNotFoundException
import io.infinitic.exceptions.serialization.JsonDeserializationException
import io.infinitic.exceptions.serialization.KotlinDeserializationException
import io.infinitic.exceptions.serialization.MissingMetaJavaClassException
import io.infinitic.exceptions.serialization.SerializerNotFoundException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializerOrNull
import java.math.BigInteger
import java.security.MessageDigest
import io.infinitic.common.serDe.json.Json as JsonJackson

@Serializable @AvroNamespace("io.infinitic.data")
data class SerializedData(
    var bytes: ByteArray,
    var type: SerializedDataType,
    val meta: Map<String, ByteArray> = mapOf()
) {
    companion object {
        // meta key containing the name of the serialized java class
        const val META_JAVA_CLASS = "javaClass"

        // use a less obvious key than "type" for polymorphic data, to avoid collusion
        private val jsonKotlin = kotlinx.serialization.json.Json {
            classDiscriminator = "#klass"
            ignoreUnknownKeys = true
        }

        /**
         * @return serialized value
         */
        @OptIn(InternalSerializationApi::class)
        fun <T : Any> from(value: T?): SerializedData {
            val bytes: ByteArray
            val type: SerializedDataType
            val meta: Map<String, ByteArray>

            when (value) {
                null -> {
                    bytes = "null".toByteArray()
                    type = SerializedDataType.NULL
                    meta = mapOf()
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    when (val serializer = value::class.serializerOrNull()?. let { it as KSerializer<T> }) {
                        null -> {
                            bytes = JsonJackson.stringify(value).toByteArray()
                            type = SerializedDataType.JSON_JACKSON
                        }
                        else -> {
                            bytes = jsonKotlin.encodeToString(serializer, value).toByteArray()
                            type = SerializedDataType.JSON_KOTLIN
                        }
                    }
                    meta = mapOf(META_JAVA_CLASS to value::class.java.name.toByteArray(charset = Charsets.UTF_8))
                }
            }
            return SerializedData(bytes, type, meta)
        }
    }

    /**
     * @return deserialized value
     */
    @OptIn(InternalSerializationApi::class)
    fun deserialize(): Any? = when (type) {
        SerializedDataType.NULL -> null
        SerializedDataType.JSON_JACKSON -> {
            val klass = getClassObject()
            try {
                JsonJackson.parse(getJson(), klass)
            } catch (e: JsonProcessingException) {
                throw JsonDeserializationException(klass.name, causeString = e.toString())
            }
        }
        SerializedDataType.JSON_KOTLIN -> {
            val klass = getClassObject()
            val serializer = klass.kotlin.serializerOrNull() ?: throw SerializerNotFoundException(klass.name)
            try {
                jsonKotlin.decodeFromString(serializer, getJson())
            } catch (e: SerializationException) {
                throw KotlinDeserializationException(klass.name, causeString = e.toString())
            }
        }
    }

    fun getJson(): String = String(bytes, Charsets.UTF_8)

    fun hash(): String {
        // MD5 implementation, enough to avoid collision in practical cases
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
    }

    /**
     * Readable version
     */
    override fun toString() = mapOf(
        "bytes" to String(bytes),
        "type" to type,
        "meta" to meta.mapValues { String(it.value) }
    ).toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedData

        if (!bytes.contentEquals(other.bytes)) return false
        if (type != other.type) return false

        if (meta.keys != other.meta.keys) return false
        if (meta.map { it.value.contentEquals(other.meta[it.key]!!) }.any { !it }) return false

        return true
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    private fun getClassName(): String? = meta[META_JAVA_CLASS]?.let { String(it, charset = Charsets.UTF_8) }

    private fun getClassObject(): Class<out Any> {
        val klassName = getClassName() ?: throw MissingMetaJavaClassException

        return try {
            Class.forName(klassName)
        } catch (e: java.lang.ClassNotFoundException) {
            throw ClassNotFoundException(klassName)
        }
    }
}
