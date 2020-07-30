package com.zenaton.workflowManager.pulsar.utils

import com.zenaton.jobManager.common.data.JobId
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.data.actions.ActionId
import com.zenaton.workflowManager.data.steps.AvroStepCriterion
import com.zenaton.workflowManager.data.steps.StepCriterion
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass

/*
 * Duplicate from com.zenaton.jobManager.utils
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
            .randomize(AvroStepCriterion::class.java) { AvroConverter.toAvroStepCriterion(randomStepCriterion()) }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }

    fun randomStepCriterion(): StepCriterion {
        val criteria = stepCriteria().values.toList()
        return criteria[Random.nextInt(until = criteria.size - 1)]
    }

    fun stepCriteria(): Map<String, StepCriterion> {
        fun getStepId() = StepCriterion.Id(ActionId(JobId()))
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val stepD = getStepId()

        return mapOf(
            "A" to stepA,
            "OR B" to StepCriterion.Or(listOf(stepA)),
            "AND A" to StepCriterion.And(listOf(stepA)),
            "A AND B" to StepCriterion.And(listOf(stepA, stepB)),
            "A OR B" to StepCriterion.Or(listOf(stepA, stepB)),
            "A OR (B OR C)" to StepCriterion.Or(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC)))),
            "A AND (B OR C)" to StepCriterion.And(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC)))),
            "A AND (B AND C)" to StepCriterion.And(listOf(stepA, StepCriterion.And(listOf(stepB, stepC)))),
            "A OR (B AND (C OR D))" to StepCriterion.Or(listOf(stepA, StepCriterion.And(listOf(stepB, StepCriterion.Or(listOf(stepC, stepD))))))
        )
    }
}
