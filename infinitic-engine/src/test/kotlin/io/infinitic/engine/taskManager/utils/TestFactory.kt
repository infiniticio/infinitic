package io.infinitic.engine.taskManager.utils

import io.infinitic.common.data.SerializedData
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.avro.taskManager.data.AvroSerializedData
import io.infinitic.avro.taskManager.data.AvroSerializedDataType
import io.kotest.properties.nextPrintableString
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer

object TestFactory {
    private var seed = 0L

    fun seed(seed: Long): TestFactory {
        TestFactory.seed = seed

        return this
    }

    fun <T : Any> random(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
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

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }
}
