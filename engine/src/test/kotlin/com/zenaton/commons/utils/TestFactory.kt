package com.zenaton.commons.utils

import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer

object TestFactory {
    private var seed = 0L

    init { seed(0L) }

    private fun seed(seed: Long): TestFactory {
        this.seed = seed
        return this
    }

    fun <T : Any> get(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }
}
