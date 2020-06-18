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
    fun serialize(data: SpecificRecord): ByteBuffer {
        val baos = ByteArrayOutputStream()
        val outputDatumWriter = SpecificDatumWriter<SpecificRecord>(data.schema)
        val encoder = EncoderFactory.get().binaryEncoder(baos, null)
        outputDatumWriter.write(data, encoder)
        encoder.flush()
        return ByteBuffer.wrap(baos.toByteArray())
    }

    inline fun <reified T : SpecificRecord> deserialize(data: ByteBuffer): T {
        // transform ByteBuffer to bytes[]
        data.rewind()
        val bytes = ByteArray(data.remaining())
        data.get(bytes, 0, bytes.size)
        // read data
        val reader = SpecificDatumReader(T::class.java)
        val binaryDecoder = DecoderFactory.get().binaryDecoder(bytes, null)
        return reader.read(null, binaryDecoder)
    }
}
