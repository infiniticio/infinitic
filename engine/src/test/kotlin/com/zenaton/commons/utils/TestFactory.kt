package com.zenaton.commons.utils

import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters

object TestFactory {
    private lateinit var easyRandom: EasyRandom

    init { seed(0L) }

    private fun seed(seed: Long) {
        val parameters = EasyRandomParameters()
            .seed(seed)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }

        easyRandom = EasyRandom(parameters)
    }

    fun <T : Any> get(klass: KClass<T>): T {
        return easyRandom.nextObject(klass.java)
    }
}
