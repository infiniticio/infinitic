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

package io.infinitic.common.serDe.avro

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.SchemasUti.getAllSchemas
import io.infinitic.common.exceptions.thisShouldNotHappen
import kotlinx.serialization.KSerializer
import org.apache.avro.Schema
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.message.BadHeaderException
import org.apache.avro.message.BinaryMessageDecoder
import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.util.RandomData
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * This object provides methods to serialize/deserialize an Avro-generated class
 */
object AvroSerDe {

    /**
     * BinaryMessageEncoder cache by class serializer
     */
    private val binaryMessageEncoders = ConcurrentHashMap<KSerializer<*>, BinaryMessageEncoder<GenericRecord>>()

    /**
     * BinaryMessageDecoder cache by class serializer
     */
    val binaryMessageDecoders = ConcurrentHashMap<KSerializer<*>, BinaryMessageDecoder<GenericRecord>>()

    /**
     * Schema cache by class serializer
     */
    private val schemas = ConcurrentHashMap<KSerializer<*>, Schema>()

    /**
     * Get schema from the serializer
     */
    fun <T> schema(serializer: KSerializer<T>): Schema =
        schemas.getOrPut(serializer) { Avro.default.schema(serializer) }

    /**
     * Object -> Avro binary with schema fingerprint
     */
    fun <T : Any> writeBinaryWithSchemaFingerprint(t: T, serializer: KSerializer<T>): ByteArray {
        val record: GenericRecord = Avro.default.toRecord(serializer, t)

        val encoder = binaryMessageEncoders.getOrPut(serializer) {
            BinaryMessageEncoder<GenericRecord>(GenericData.get(), schema(serializer))
        }
        val out = ByteArrayOutputStream()
        encoder.encode(record, out)

        return out.toByteArray()
    }

    /**
     * Avro binary with schema fingerprint -> Object
     */
    inline fun <reified T : Any> readBinaryWithSchemaFingerprint(bytes: ByteArray, serializer: KSerializer<T>): T {
        val decoder = binaryMessageDecoders.getOrPut(serializer) {
            BinaryMessageDecoder<GenericRecord>(GenericData.get(), schema(serializer)).also { decoder ->
                getAllSchemas<T>().also { it.values.forEach { schema -> decoder.addSchema(schema) } }
            }
        }

        return try {
            val record = decoder.decode(SeekableByteArrayInput(bytes), null)

            Avro.default.fromRecord(serializer, record)
        } catch (e: BadHeaderException) {
            // we try to decode binary using all known schemas
            // this is to support the case where the binary has been created without schema fingerprint (<= 0.9.7)
            run breaking@{
                getAllSchemas<T>().forEach { (_, schema) ->
                    try {
                        // let's try to decode with this schema
                        return@breaking readBinary(bytes, serializer, schema)
                    } catch (e: Exception) {
                        // go to next schema
                    }
                }
                thisShouldNotHappen("No adhoc schema found for $serializer")
            }
        }
    }

    /**
     * Object -> Avro binary
     */
    fun <T : Any> writeBinary(t: T, serializer: KSerializer<T>): ByteArray {
        val record: GenericRecord = Avro.default.toRecord(serializer, t)

        val datumWriter = GenericDatumWriter<GenericRecord>(schema(serializer))
        val out = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        datumWriter.write(record, encoder)
        encoder.flush()

        return out.toByteArray()
    }

    /**
     * Avro binary + schema -> Object
     */
    fun <T> readBinary(bytes: ByteArray, serializer: KSerializer<T>, readerSchema: Schema): T {
        val datumReader = GenericDatumReader<GenericRecord>(readerSchema)
        val decoder = DecoderFactory.get().binaryDecoder(SeekableByteArrayInput(bytes), null)
        val record = datumReader.read(null, decoder)

        return Avro.default.fromRecord(serializer, record)
    }

    @TestOnly
    fun getRandomBinaryWithSchemaFingerprint(schema: Schema): ByteArray {
        val o = RandomData(schema, 1).first()

        val encoder = BinaryMessageEncoder<Any>(GenericData.get(), schema)
        val out = ByteArrayOutputStream()
        encoder.encode(o, out)

        return out.toByteArray()
    }
}
