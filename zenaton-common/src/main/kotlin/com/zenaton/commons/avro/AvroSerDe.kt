package com.zenaton.common.avro

import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificRecord
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * This object provides methods to serialize/deserialize an Avro-generated class
 */
object AvroSerDe {
    fun serializeToByteArray(data: SpecificRecord): ByteArray {
        val out = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        val writer = SpecificDatumWriter<SpecificRecord>(data.schema)
        writer.write(data, encoder)
        encoder.flush()
        out.close()
        return out.toByteArray()
    }

    inline fun <reified T : SpecificRecord> deserializeFromByteArray(bytes: ByteArray): T {
        val reader = SpecificDatumReader(T::class.java)
        val binaryDecoder = DecoderFactory.get().binaryDecoder(bytes, null)
        return reader.read(null, binaryDecoder)
    }

    fun serialize(data: SpecificRecord): ByteBuffer {
        return ByteBuffer.wrap(serializeToByteArray(data))
    }

    inline fun <reified T : SpecificRecord> deserialize(data: ByteBuffer): T {
        // transform ByteBuffer to bytes[]
        data.rewind()
        val bytes = ByteArray(data.remaining())
        data.get(bytes, 0, bytes.size)
        // read data
        return deserializeFromByteArray<T>(bytes)
    }
}
