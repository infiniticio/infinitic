package com.zenaton.workflowManager

import com.zenaton.workflowManager.avro.AvroConverter
import com.zenaton.workflowManager.data.actions.Action
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass

fun main() {

    val o1 = TestFactory.get(ForWorkflowEngineMessage::class)
    println(o1)
    val o2 = AvroConverter.toWorkflowEngine(o1)
    println(o2)
    val o3 = AvroConverter.fromWorkflowEngine(o2)
    println(o3)
    val o4 = AvroConverter.toWorkflowEngine(o3)
    println(o4)

    fun test(klass: KClass<out Action>) {
        if (klass.isSealed) {
            klass.sealedSubclasses.forEach { test(it) }
        } else {
            val o1 = TestFactory.get(klass)
            println(o1)

            val o2 = AvroConverter.toAvro(o1)
            println(o2)

            val o3 = AvroConverter.fromAvro(o2)
            println(o1 == o3)

            val o4 = AvroConverter.toAvro(o3)
            println(o2 == o4)
        }
    }
//    test(Action::class)
}

object TestFactory {
    private var seed = 0L

    fun seed(seed: Long): TestFactory {
        TestFactory.seed = seed
        return this
    }

    fun <T : Any> get(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .scanClasspathForConcreteTypes(true)
            .seed(seed)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }
}
