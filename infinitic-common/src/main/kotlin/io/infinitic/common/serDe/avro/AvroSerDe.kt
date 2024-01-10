/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.serDe.avro

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.currentVersion
import io.infinitic.isRelease
import io.infinitic.versions
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
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
import org.apache.avro.message.MessageDecoder
import org.apache.avro.message.MessageEncoder
import org.apache.avro.message.RawMessageEncoder
import org.apache.avro.util.RandomData
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/** This object provides methods to serialize/deserialize an Avro-generated class */
@OptIn(InternalSerializationApi::class)
object AvroSerDe {

  const val SCHEMAS_FOLDER = "schemas"

  /** MessageEncoder cache by class serializer */
  private val messageEncoders = ConcurrentHashMap<KSerializer<*>, MessageEncoder<GenericRecord>>()

  /** MessageDecoder cache by class serializer */
  private val messageDecoders = ConcurrentHashMap<KSerializer<*>, MessageDecoder<GenericRecord>>()

  /** Schema cache by class serializer */
  private val currentSchemas = ConcurrentHashMap<KSerializer<*>, Schema>()

  /** Schema cache by class serializer */
  private val allSchemas = ConcurrentHashMap<KClass<*>, Map<String, Schema>>()

  /** Get schema from the serializer */
  fun <T> currentSchema(serializer: KSerializer<T>): Schema =
      currentSchemas.getOrPut(serializer) { Avro.default.schema(serializer) }

  /** Get message encoder from the serializer */
  private fun <T> messageEncoder(serializer: KSerializer<T>): MessageEncoder<GenericRecord> =
      messageEncoders.getOrPut(serializer) {
        BinaryMessageEncoder(GenericData.get(), currentSchema(serializer))
      }

  /** Get message decoder from the serializer */
  private fun <T : Any> messageDecoder(
    klass: KClass<T>,
    serializer: KSerializer<T>
  ): MessageDecoder<GenericRecord> = messageDecoders.getOrPut(serializer) {
    BinaryMessageDecoder<GenericRecord>(
        GenericData.get(),
        currentSchema(serializer),
    ).also { decoder ->
      getAllSchemas(klass).values.forEach { schema -> decoder.addSchema(schema) }
    }
  }

  /** Object -> Avro binary with schema fingerprint */
  fun <T : Any> writeBinaryWithSchemaFingerprint(
    t: T,
    serializer: KSerializer<T>
  ): ByteArray {
    val record: GenericRecord = Avro.default.toRecord(serializer, currentSchema(serializer), t)
    val encoder = messageEncoder(serializer)
    val out = ByteArrayOutputStream()
    encoder.encode(record, out)

    return out.toByteArray()
  }

  /** Avro binary with schema fingerprint -> Object */
  fun <T : Any> readBinaryWithSchemaFingerprint(
    bytes: ByteArray,
    klass: KClass<T>,
  ): T {
    val serializer: KSerializer<T> = klass.serializerOrNull() ?: thisShouldNotHappen()
    val decoder = messageDecoder(klass, serializer)

    return try {
      val record = decoder.decode(SeekableByteArrayInput(bytes), null)
      Avro.default.fromRecord(serializer, record)
    } catch (e: BadHeaderException) {
      // we try to decode binary using all known schemas
      // in case binary was created without schema fingerprint (<= 0.9.7)
      run breaking@{
        getAllSchemas(klass).forEach { (_, schema) ->
          try {
            // let's try to decode with this schema
            return@breaking readBinary(bytes, schema, serializer)
          } catch (e: Exception) {
            // go to next schema
          }
        }
        thisShouldNotHappen("No adhoc schema found for $serializer")
      }
    }
  }

  /** Object -> Avro binary */
  fun <T : Any> writeBinary(t: T, serializer: KSerializer<T>): ByteArray {
    val record = Avro.default.toRecord(serializer, currentSchema(serializer), t)
    val datumWriter = GenericDatumWriter<GenericRecord>(currentSchema(serializer))
    val out = ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    datumWriter.write(record, encoder)
    encoder.flush()

    return out.toByteArray()
  }

  /** Avro binary + schema -> Object */
  fun <T> readBinary(bytes: ByteArray, readerSchema: Schema, serializer: KSerializer<T>): T {
    val datumReader = GenericDatumReader<GenericRecord>(readerSchema)
    val decoder = DecoderFactory.get().binaryDecoder(SeekableByteArrayInput(bytes), null)
    val record = datumReader.read(null, decoder)

    return Avro.default.fromRecord(serializer, record)
  }

  /** Get prefix of schema file in Resources */
  fun <T : Any> getSchemaFilePrefix(klass: KClass<T>) =
      klass.simpleName!!.replaceFirstChar { it.lowercase() }

  /** Get map <version, schema> for all schemas of type T */
  fun <T : Any> getAllSchemas(klass: KClass<T>): Map<String, Schema> = allSchemas.getOrPut(klass) {
    val prefix = getSchemaFilePrefix(klass)

    val schemas = versions.map { version ->
      val url = "/$SCHEMAS_FOLDER/$prefix-$version.avsc"
      this::class.java.getResource(url)?.readText()
    }

    val released = versions.zip(schemas)
        .filter { (_, schema) -> schema != null }
        .toMap()
        .mapValues { (_, schema) -> Schema.Parser().parse(schema) }
        as MutableMap

    // add current version if not already there
    if (!isRelease) {
      released[currentVersion] = currentSchema(klass.serializer())
    }

    return released
  }

  @TestOnly
  fun getRandomBinaryWithSchemaFingerprint(schema: Schema): ByteArray {
    val o = RandomData(schema, 1).first()
    val encoder = BinaryMessageEncoder<Any>(GenericData.get(), schema)
    val out = ByteArrayOutputStream()
    encoder.encode(o, out)

    return out.toByteArray()
  }

  @TestOnly
  fun getRandomBinary(schema: Schema): ByteArray {
    val o = RandomData(schema, 1).first()
    val encoder = RawMessageEncoder<Any>(GenericData.get(), schema)
    val out = ByteArrayOutputStream()
    encoder.encode(o, out)

    return out.toByteArray()
  }
}
