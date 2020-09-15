package io.infinitic.taskManager.common.utils

import io.infinitic.common.data.SerializedData
import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.data.TaskAttemptError
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.exceptions.CanNotUseJavaReservedKeywordInMeta
import io.infinitic.taskManager.common.exceptions.MultipleMethodCallsAtDispatch
import io.infinitic.taskManager.data.AvroSerializedData
import io.infinitic.taskManager.data.AvroSerializedDataType
import io.kotest.properties.nextPrintableString
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer

fun main() {
    val e = TestFactory.random(MultipleMethodCallsAtDispatch::class)
    println(e.cause)
    val s = Json.stringify(e)
    println(s)
}
object TestFactory {
    private var seed = 0L

    fun seed(seed: Long): TestFactory {
        this.seed = seed

        return this
    }

    fun <T : Any> random(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .collectionSizeRange(1, 5)
            .scanClasspathForConcreteTypes(true)
            .overrideDefaultInitialization(true)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }
            .randomize(ByteArray::class.java) { Random(seed).nextBytes(10) }
            .randomize(SerializedData::class.java) { SerializedData.from(Random(seed).nextPrintableString(10)) }
            .randomize(AvroSerializedData::class.java) {
                val data = random(SerializedData::class)
                AvroSerializedData.newBuilder()
                    .setBytes(ByteBuffer.wrap(data.bytes))
                    .setType(AvroSerializedDataType.JSON)
                    .setMeta(data.meta.mapValues { ByteBuffer.wrap(it.value) })
                    .build()
            }
            .randomize(TaskInput::class.java) {
                TaskInput(
                    Random(seed).nextBytes(10),
                    Random(seed).nextPrintableString(10)
                )
            }
            .randomize(TaskOutput::class.java) {
                TaskOutput(
                    Random(seed).nextPrintableString(10)
                )
            }
            .randomize(TaskAttemptError::class.java) {
                TaskAttemptError(CanNotUseJavaReservedKeywordInMeta("foo"))
            }
            .randomize(TaskMeta::class.java) {
                TaskMeta(mutableMapOf("foo" to "bar"))
            }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }
}
