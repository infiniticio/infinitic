package io.infinitic.common

import com.sksamuel.avro4k.Avro
import com.sksamuel.avro4k.io.AvroFormat
import io.infinitic.common.data.SerializedData
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalEnvelope
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.taskEngineMessages.CancelTask
import io.infinitic.common.tasks.messages.taskEngineMessages.RetryTask
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineEnvelope
import io.infinitic.common.tasks.messages.workerMessages.WorkerEnvelope
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.apache.avro.Schema
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

@InternalSerializationApi
fun main() {

   val msg = RetryTask(TaskId("ertyu"), null, null,
       null,
       null, null, null)
    println(Json.encodeToString(msg))


//    val schema: Schema = Avro.default.schema(TaskEngineEnvelope.serializer())
//    println(schema.toString(true))
//
//    // val m = Test(mapOf("foo" to "bar".toByteArray()))
//    val m = TestFactory.random<TaskEngineEnvelope>()
//    println(m)
//    println(m.deepcopy())
//
//    val json = Json.encodeToString(m)
//
//    println(json)
//
//    val m2 = Json.decodeFromString<TaskEngineEnvelope>(json)
//    println(m2)
//
//    println(m2 == m)

}

@Serializable
data class Test(
    val meta: Map<String, ByteArray> = mapOf()
)


fun TaskEngineEnvelope.deepcopy(): TaskEngineEnvelope {
    val serializer = TaskEngineEnvelope.serializer()
    val byteArray =  Avro.default.encodeToByteArray(serializer, this)

    return Avro.default.decodeFromByteArray(serializer, byteArray)
}

fun Test.deepcopy(): Test {
    val serializer = Test.serializer()
    val byteArray =  Avro.default.encodeToByteArray(serializer, this)

    return Avro.default.decodeFromByteArray(serializer, byteArray)
}

fun SerializedData.deepcopy2(): SerializedData {
    val serializer = SerializedData.serializer()
    val byteArray =  writeBinary(this, serializer)

    return readBinary(byteArray, serializer)
}

fun SerializedData.deepcopy(): SerializedData {
    val serializer = SerializedData.serializer()
    val byteArray =  Avro.default.encodeToByteArray(serializer, this)

    return Avro.default.decodeFromByteArray(serializer, byteArray)
}

fun CancelTask.deepcopy(): CancelTask {
    val serializer = CancelTask.serializer()
    val byteArray =  Avro.default.encodeToByteArray(serializer, this)

    return Avro.default.decodeFromByteArray(serializer, byteArray)
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
    return  Avro.default.fromRecord(serializer, datumReader.read(null, decoder))
}
