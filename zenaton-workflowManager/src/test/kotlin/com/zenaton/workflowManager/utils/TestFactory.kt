package com.zenaton.workflowManager.utils

import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.avro.AvroConverter
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

    fun <T : Any> get(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .scanClasspathForConcreteTypes(true)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }
            .randomize(AvroStepCriterion::class.java) { AvroConverter.toAvroStepCriterion(randomStepCriterion()) }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }

    fun randomStepCriterion(): StepCriterion {
        fun getStepId() = StepCriterion.Id(ActionId(JobId()))
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val stepD = getStepId()
        return when (Random.nextInt(until = 8)) {
            0 -> stepA
            1 -> StepCriterion.Or(listOf(stepA))
            2 -> StepCriterion.And(listOf(stepA))
            3 -> StepCriterion.And(listOf(stepA, stepB))
            4 -> StepCriterion.Or(listOf(stepA, stepB))
            5 -> StepCriterion.Or(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))
            6 -> StepCriterion.And(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))
            7 -> StepCriterion.And(listOf(stepA, StepCriterion.And(listOf(stepB, stepC))))
            8 -> StepCriterion.Or(listOf(stepA, StepCriterion.And(listOf(stepB, StepCriterion.Or(listOf(stepC, stepD))))))
            else -> throw Exception("This should not happen")
        }
    }
}
