/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.fixtures

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.Version
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.events.messages.ServiceEventEnvelope
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.errors.DeferredError
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatus
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEventEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEventMessage
import io.infinitic.tasks.TaskExceptionDetail
import io.infinitic.tasks.TaskFailure
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass

fun methodParametersFrom(vararg data: Any?) =
    MethodArgs(data.map { SerializedData.encode(it, (it ?: "")::class.java, null) }.toList())

object TestFactory {
  private var seed = 0L

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
        .randomize(NewStep::class.java) { NewStep(step = random(), stepPosition = random()) }
        .randomize(Step.Id::class.java) { Step.Id(random(), random()) }
        .randomize(StepStatus.Completed::class.java) { StepStatus.Completed(random(), random()) }
        .randomize(ByteArray::class.java) { Random(seed).nextBytes(10) }
        .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(random()) }
        .randomize(Version::class.java) { Version(random<String>()) }
        .randomize(SerializedData::class.java) {
          SerializedData.encode(random<String>(), String::class.java, null)
        }
        .randomize(TaskFailure::class.java) {
          TaskFailure(
              random(),
              retrySequence = 0,
              retryIndex = 0,
              exceptionDetail = random(),
              previousFailure = null,
          )
        }
        .randomize(TaskExceptionDetail::class.java) {
          TaskExceptionDetail(random(), random(), random(), emptyMap(), null)
        }
        .randomize(MethodArgs::class.java) {
          methodParametersFrom(random<ByteArray>(), random<String>())
        }
        .randomize(WorkflowEngineEnvelope::class.java) {
          val sub = WorkflowStateEngineMessage::class.sealedSubclasses.shuffled().first()
          WorkflowEngineEnvelope.from(random(sub))
        }
        .randomize(WorkflowEventEnvelope::class.java) {
          val sub = WorkflowStateEventMessage::class.sealedSubclasses.shuffled().first()
          WorkflowEventEnvelope.from(random(sub))
        }
        .randomize(ServiceEventEnvelope::class.java) {
          val sub = ServiceExecutorEventMessage::class.sealedSubclasses.shuffled().first()
          ServiceEventEnvelope.from(random(sub))
        }
        .randomize(DeferredError::class.java) {
          val sub = DeferredError::class.sealedSubclasses.shuffled().first()
          random(sub)
        }
        .randomize(MessageId::class.java) { MessageId() }

    values?.forEach { parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value }) }

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
        "A OR (B AND (C OR D))" to
            Step.Or(listOf(stepA, Step.And(listOf(stepB, Step.Or(listOf(stepC, stepD)))))),
    )
  }

  private fun randomStep(): Step {
    val steps = steps().values.toList()
    return steps[Random.nextInt(until = steps.size - 1)]
  }
}
