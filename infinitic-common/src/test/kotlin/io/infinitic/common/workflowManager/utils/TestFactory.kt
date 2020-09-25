package io.infinitic.common.workflowManager.utils

import io.infinitic.common.data.SerializedData
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.avro.taskManager.data.AvroSerializedData
import io.infinitic.avro.taskManager.data.AvroSerializedDataType
import io.infinitic.common.workflowManager.avro.AvroConverter
import io.infinitic.common.workflowManager.data.commands.CommandId
import io.infinitic.common.workflowManager.data.commands.CommandStatusOngoing
import io.infinitic.common.workflowManager.data.methodRuns.MethodInput
import io.infinitic.common.workflowManager.data.steps.Step
import io.infinitic.avro.workflowManager.data.steps.AvroStep
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

    inline fun <reified T : Any> random(values: Map<String, Any?>? = null) = random(T::class, values)

    fun <T : Any> random(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .scanClasspathForConcreteTypes(true)
            .overrideDefaultInitialization(true)
            // for "Any" parameter, provides a String
            .randomize(Any::class.java) { Random(seed).nextPrintableString(10) }
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
            .randomize(MethodInput::class.java) { MethodInput(Random(seed).nextPrintableString(10)) }
            .randomize(TaskInput::class.java) { TaskInput(Random(seed).nextBytes(10)) }
            .randomize(AvroStep::class.java) { AvroConverter.convertJson(randomStep()) }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }

    private fun randomStep(): Step {
        val steps = steps().values.toList()
        return steps[Random.nextInt(until = steps.size - 1)]
    }

    internal fun steps(): Map<String, Step> {
        fun getStepId() = Step.Id(CommandId(), CommandStatusOngoing)
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