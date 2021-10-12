/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.fixtures

import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.errors.Error
import io.infinitic.common.metrics.global.messages.MetricsGlobalEnvelope
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.perName.messages.MetricsPerNameEnvelope
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass

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
            .collectionSizeRange(1, 5)
            .randomize(Any::class.java) { random<String>() }
            .randomize(Step::class.java) { randomStep() }
            .randomize(String::class.java) { String(random(), Charsets.UTF_8) }
            .randomize(ByteArray::class.java) { Random(seed).nextBytes(10) }
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(random()) }
            .randomize(MethodParameters::class.java) { MethodParameters.from(random<ByteArray>(), random<String>()) }
            .randomize(SerializedData::class.java) { SerializedData.from(random<String>()) }
            .randomize(WorkflowEngineEnvelope::class.java) {
                val sub = WorkflowEngineMessage::class.sealedSubclasses.shuffled().first()
                WorkflowEngineEnvelope.from(random(sub))
            }
            .randomize(TaskEngineEnvelope::class.java) {
                val sub = TaskEngineMessage::class.sealedSubclasses.shuffled().first()
                TaskEngineEnvelope.from(random(sub))
            }
            .randomize(MetricsPerNameEnvelope::class.java) {
                val sub = MetricsPerNameMessage::class.sealedSubclasses.shuffled().first()
                MetricsPerNameEnvelope.from(random(sub))
            }
            .randomize(MetricsGlobalEnvelope::class.java) {
                val sub = MetricsGlobalMessage::class.sealedSubclasses.shuffled().first()
                MetricsGlobalEnvelope.from(random(sub))
            }
            .randomize(Error::class.java) {
                Error(
                    errorName = random(),
                    errorStackTraceToString = random(),
                    errorMessage = random(),
                    errorCause = null,
                    whereId = null,
                    whereName = null
                )
            }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }

    private fun steps(): Map<String, Step> {
        fun getStepId() = Step.Id(CommandId())
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

    private fun randomStep(): Step {
        val steps = steps().values.toList()
        return steps[Random.nextInt(until = steps.size - 1)]
    }
}
