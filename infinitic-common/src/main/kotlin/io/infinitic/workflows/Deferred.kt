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

package io.infinitic.workflows

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.common.workflows.data.steps.Step
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import io.infinitic.common.workflows.data.steps.and as stepAnd
import io.infinitic.common.workflows.data.steps.or as stepOr

@Serializable(with = DeferredSerializer::class)
data class Deferred<T> (val step: Step) {
    @Transient @JsonIgnore lateinit var workflowDispatcher: WorkflowDispatcher

    @Transient @JsonIgnore val id: String? = when (step) {
        is Step.Id -> step.commandId.toString()
        else -> null
    }

    companion object {
        private val workflowDispatcherLocal: ThreadLocal<WorkflowDispatcher> = ThreadLocal()

        fun setWorkflowDispatcher(workflowDispatcher: WorkflowDispatcher) =
            workflowDispatcherLocal.set(workflowDispatcher)

        fun delWorkflowDispatcher() = workflowDispatcherLocal.set(null)
    }

    init {
        // special way to initialize workflowDispatcher when deserializing Deferred in WorkflowTaskImpl
        workflowDispatcherLocal.get()?.let { workflowDispatcher = it }
    }

    /**
     * Wait the completion or cancellation of a deferred and get its result
     */
    fun await(): T = workflowDispatcher.await(this)

    /**
     * Status of a deferred
     */
    fun status(): DeferredStatus = workflowDispatcher.status(this)

    @JsonIgnore fun isCompleted() = status() == DeferredStatus.COMPLETED

    @JsonIgnore fun isCanceled() = status() == DeferredStatus.CANCELED

    @JsonIgnore fun isOngoing() = status() == DeferredStatus.ONGOING
}

object DeferredSerializer : KSerializer<Deferred<*>> {
    override val descriptor: SerialDescriptor = Step.serializer().descriptor
    override fun serialize(encoder: Encoder, value: Deferred<*>) { encoder.encodeSerializableValue(Step.serializer(), value.step) }
    override fun deserialize(decoder: Decoder): Deferred<*> = Deferred<Any>(decoder.decodeSerializableValue(Step.serializer()))
}

fun or(vararg others: Deferred<*>) = others.reduce { acc, deferred -> acc or deferred }

fun and(vararg others: Deferred<*>) = others.reduce { acc, deferred -> acc and deferred }

@JvmName("orT0")
infix fun <T> Deferred<out T>.or(other: Deferred<out T>) =
    Deferred<T>(stepOr(step, other.step)).apply { workflowDispatcher = this@or.workflowDispatcher }

@JvmName("orT1")
infix fun <T> Deferred<List<T>>.or(other: Deferred<out T>) =
    Deferred<Any>(stepOr(step, other.step)).apply { workflowDispatcher = this@or.workflowDispatcher }

@JvmName("orT2")
infix fun <T> Deferred<List<T>>.or(other: Deferred<List<T>>) =
    Deferred<List<T>>(stepOr(step, other.step)).apply { workflowDispatcher = this@or.workflowDispatcher }

@JvmName("orT3")
infix fun <T> Deferred<out T>.or(other: Deferred<List<T>>) =
    Deferred<Any>(stepOr(step, other.step)).apply { workflowDispatcher = this@or.workflowDispatcher }

@JvmName("andT0")
infix fun <T> Deferred<out T>.and(other: Deferred<out T>) =
    Deferred<List<T>>(stepAnd(step, other.step)).apply { workflowDispatcher = this@and.workflowDispatcher }

@JvmName("andT1")
infix fun <T> Deferred<List<T>>.and(other: Deferred<out T>) =
    Deferred<List<T>>(stepAnd(step, other.step)).apply { workflowDispatcher = this@and.workflowDispatcher }

@JvmName("andT2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(stepAnd(step, other.step)).apply { workflowDispatcher = this@and.workflowDispatcher }

@JvmName("andT3")
infix fun <T> Deferred<out T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(stepAnd(step, other.step)).apply { workflowDispatcher = this@and.workflowDispatcher }

// extension function to apply AND to a List<Deferred<T>>
fun <T> List<Deferred<T>>.and() =
    Deferred<List<T>>(Step.And(map { it.step })).apply { workflowDispatcher = first().workflowDispatcher }

// extension function to apply OR to a List<Deferred<T>>
fun <T> List<Deferred<T>>.or() =
    Deferred<T>(Step.Or(map { it.step })).apply { workflowDispatcher = first().workflowDispatcher }
