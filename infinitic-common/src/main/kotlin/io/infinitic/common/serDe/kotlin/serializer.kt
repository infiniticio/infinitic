package io.infinitic.common.serDe.kotlin

import com.sksamuel.avro4k.Avro
import com.sksamuel.avro4k.io.AvroFormat
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.common.workers.messages.WorkerEnvelope
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import java.io.ByteArrayOutputStream
import java.lang.reflect.Modifier.isStatic
import kotlin.reflect.KClass

// @OptIn(ExperimentalStdlibApi::class)
// fun getKSerializerOrNull(klass: Class<*>) = try {
//    @Suppress("UNCHECKED_CAST")
//    serializer(klass.kotlin.createType())
// } catch (e: Exception) {
//    null
// }

fun getKSerializerOrNull(klass: Class<*>): KSerializer<*>? {
    val companionField = klass.declaredFields.find {
        it.name == "Companion" && isStatic(it.modifiers)
    } ?: return null
    val companion = companionField.get(klass)
    val serializerMethod = try {
        companion::class.java.getMethod("serializer")
    } catch (e: NoSuchMethodException) {
        return null
    }
    if (serializerMethod.returnType.name != KSerializer::class.qualifiedName) {
        return null
    }
    @Suppress("UNCHECKED_CAST")
    return serializerMethod.invoke(companion) as KSerializer<*>
}

@Suppress("UNCHECKED_CAST")
fun <T: Any> kserializer(klass: KClass<T>) = when (klass) {
    MonitoringGlobalEnvelope::class -> MonitoringGlobalEnvelope.serializer()
    MonitoringPerNameEnvelope::class -> MonitoringPerNameEnvelope.serializer()
    TaskEngineEnvelope::class-> TaskEngineEnvelope.serializer()
    WorkflowEngineEnvelope::class-> WorkflowEngineEnvelope.serializer()
    WorkerEnvelope::class -> WorkerEnvelope.serializer()
    else -> throw RuntimeException("This should not happen: apply kserializer with  ${klass.qualifiedName}")
} as KSerializer <T>


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
