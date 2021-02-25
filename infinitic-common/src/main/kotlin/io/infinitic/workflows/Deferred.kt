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

import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatus
import io.infinitic.common.workflows.data.steps.and as stepAnd
import io.infinitic.common.workflows.data.steps.or as stepOr

data class Deferred<T> (
    val step: Step,
    internal val workflowTaskContext: WorkflowTaskContext
) {
    val id: String?
        get() = when (step) {
            is Step.Id -> "${step.commandId}"
            else -> null
        }

    lateinit var stepStatus: StepStatus

    /*
     * Use this method to wait the completion or cancellation of a deferred and get its result
     */
    fun await(): T = workflowTaskContext.await(this)

    /*
     * Use this method to get the status of a deferred
     */
    fun status(): DeferredStatus = workflowTaskContext.status(this)
}

fun or(vararg others: Deferred<*>) = others.reduce { acc, deferred -> acc or deferred }

fun and(vararg others: Deferred<*>) = others.reduce { acc, deferred -> acc and deferred }

@JvmName("orT0")
infix fun <T> Deferred<out T>.or(other: Deferred<out T>) =
    Deferred<T>(stepOr(this.step, other.step), this.workflowTaskContext)

@JvmName("orT1")
infix fun <T> Deferred<List<T>>.or(other: Deferred<out T>) =
    Deferred<Any>(stepOr(this.step, other.step), this.workflowTaskContext)

@JvmName("orT2")
infix fun <T> Deferred<List<T>>.or(other: Deferred<List<T>>) =
    Deferred<List<T>>(stepOr(this.step, other.step), this.workflowTaskContext)

@JvmName("orT3")
infix fun <T> Deferred<out T>.or(other: Deferred<List<T>>) =
    Deferred<Any>(stepOr(this.step, other.step), this.workflowTaskContext)

@JvmName("andT0")
infix fun <T> Deferred<out T>.and(other: Deferred<out T>) =
    Deferred<List<T>>(stepAnd(this.step, other.step), this.workflowTaskContext)

@JvmName("andT1")
infix fun <T> Deferred<List<T>>.and(other: Deferred<out T>) =
    Deferred<List<T>>(stepAnd(this.step, other.step), this.workflowTaskContext)

@JvmName("andT2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(stepAnd(this.step, other.step), this.workflowTaskContext)

@JvmName("andT3")
infix fun <T> Deferred<out T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(stepAnd(this.step, other.step), this.workflowTaskContext)

// extension function to apply AND to a List<Deferred<T>>
fun <T> List<Deferred<T>>.and() =
    Deferred<List<T>>(Step.And(this.map { it.step }), this.first().workflowTaskContext)

// extension function to apply OR to a List<Deferred<T>>
fun <T> List<Deferred<T>>.or() =
    Deferred<T>(Step.Or(this.map { it.step }), this.first().workflowTaskContext)
