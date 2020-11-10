package io.infinitic.common.serDe.kotlin

import com.sksamuel.avro4k.Avro
import com.sksamuel.avro4k.io.AvroFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import java.io.ByteArrayOutputStream
import java.lang.reflect.Modifier.isStatic
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

//@OptIn(ExperimentalStdlibApi::class)
//fun getKSerializerOrNull(klass: Class<*>) = try {
//    @Suppress("UNCHECKED_CAST")
//    serializer(klass.kotlin.createType())
//} catch (e: Exception) {
//    null
//}

fun getKSerializerOrNull(klass: Class<*>) : KSerializer<*>? {
    val companionField = klass.declaredFields.find {
        it.name == "Companion" && isStatic(it.modifiers)
    } ?: return null
    val companion = companionField.get(klass)
    val serializerMethod = try {
        companion::class.java.getMethod("serializer")
    } catch (e: NoSuchMethodException) {
        return null
    }
    if (serializerMethod.returnType.name !=  KSerializer::class.qualifiedName) {
        return null
    }
    @Suppress("UNCHECKED_CAST")
    return serializerMethod.invoke(companion) as KSerializer<*>
}

fun <T> writeBinary(t: T, serializer: SerializationStrategy<T>): ByteArray {
    val schema = Avro.default.schema(serializer)
    val out = ByteArrayOutputStream()
    Avro.default.openOutputStream(serializer) {
        format = AvroFormat.BinaryFormat
        this.schema = schema
    }.to(out).write(t).close()
    return out.toByteArray()
}

fun <T> readBinary(bytes: ByteArray, serializer: KSerializer<T>): T {
    val schema = Avro.default.schema(serializer)
    val datumReader = GenericDatumReader<GenericRecord>(schema)
    val decoder = DecoderFactory.get().binaryDecoder(SeekableByteArrayInput(bytes), null)
    return Avro.default.fromRecord(serializer, datumReader.read(null, decoder))
}
