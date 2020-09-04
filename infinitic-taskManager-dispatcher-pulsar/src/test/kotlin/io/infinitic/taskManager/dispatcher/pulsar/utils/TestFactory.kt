package io.infinitic.taskManager.dispatcher.pulsar.utils

import io.infinitic.taskManager.common.data.TaskInput
import io.kotest.properties.nextPrintableString
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass

/*
 * Duplicate from io.infinitic.taskManager.utils
 * We should use java-test-fixtures but we can not
 * https://github.com/gradle/gradle/issues/11501
 */
object TestFactory {
    private var seed = 0L

    private fun seed(seed: Long): TestFactory {
        TestFactory.seed = seed
        return this
    }

    fun <T : Any> random(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }
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
