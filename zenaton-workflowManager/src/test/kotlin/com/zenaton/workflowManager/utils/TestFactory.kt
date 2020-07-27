package com.zenaton.workflowManager.utils

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.data.actions.ActionId
import com.zenaton.workflowManager.data.steps.AvroStepCriterion
import com.zenaton.workflowManager.data.steps.StepCriterion
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
            .randomize(SerializedData::class.java) {
                SerializedData.from(
                    "test",
                    mapOf("metakey" to "metavalue".toByteArray(charset = Charsets.UTF_8))
                )
            }
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
