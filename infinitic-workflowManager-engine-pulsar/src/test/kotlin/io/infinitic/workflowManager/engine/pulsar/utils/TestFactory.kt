package io.infinitic.workflowManager.pulsar.utils

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.workflowManager.common.data.steps.Step
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

    fun seed(seed: Long): TestFactory {
        TestFactory.seed = seed
        return this
    }

    fun <T : Any> random(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .collectionSizeRange(1, 5)
            .scanClasspathForConcreteTypes(true)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }
//            .randomize(AvroStepCriterion::class.java) { AvroConverter.toAvroStep(randomStepCriterion()) }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }

    fun randomStepCriterion(): Step {
        val steps = step().values.toList()
        return steps[Random.nextInt(until = steps.size - 1)]
    }

    fun step(): Map<String, Step> {
        fun getStepId() = Step.Id(CommandId(TaskId()), CommandStatusOngoing())
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val stepD = getStepId()

        return mapOf(
            "A" to stepA,
            "OR B" to Step.Or(listOf(stepA)),
            "AND A" to Step.And(listOf(stepA)),
            "A AND B" to Step.And(listOf(stepA, stepB)),
            "A OR B" to Step.Or(listOf(stepA, stepB)),
            "A OR (B OR C)" to Step.Or(listOf(stepA, Step.Or(listOf(stepB, stepC)))),
            "A AND (B OR C)" to Step.And(listOf(stepA, Step.Or(listOf(stepB, stepC)))),
            "A AND (B AND C)" to Step.And(listOf(stepA, Step.And(listOf(stepB, stepC)))),
            "A OR (B AND (C OR D))" to Step.Or(listOf(stepA, Step.And(listOf(stepB, Step.Or(listOf(stepC, stepD))))))
        )
    }
}
