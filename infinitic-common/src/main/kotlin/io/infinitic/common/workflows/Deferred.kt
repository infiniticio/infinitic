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

package io.infinitic.common.workflows

import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.Step.And
import io.infinitic.common.workflows.data.steps.Step.Or
import io.infinitic.common.workflows.data.steps.StepStatus

data class Deferred<T> (
    val step: Step,
    val workflowTaskContext: WorkflowTaskContext
) {
    lateinit var stepStatus: StepStatus

    /*
     * Use this method to wait the completion or cancellation of a deferred
     */
    fun await() = workflowTaskContext.await(this)

    /*
     * Use this method to wait the completion or cancellation of a deferred
     * and get its result
     */
    fun result() = workflowTaskContext.result(this)

    /*
     * Use this method to get the status of a deferred
     */
    fun status() = workflowTaskContext.status(this)
}

// infix functions to compose Deferred
@JvmName("orT0")
infix fun <T> Deferred<T>.or(other: Deferred<T>) =
    Deferred<T>(Or(listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("orT1")
infix fun <T> Deferred<List<T>>.or(other: Deferred<T>) =
    Deferred<Any>(Or(listOf(this.step, other.step)), this.workflowTaskContext)

// extension function to apply OR to a List<Deferred<T>>
fun <T> List<Deferred<T>>.or() =
    Deferred<T>(Or(this.map { it.step }), this.first().workflowTaskContext)

@JvmName("andT0")
infix fun <T> Deferred<T>.and(other: Deferred<T>) =
    Deferred<List<T>>(And(listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("andT1")
infix fun <T> Deferred<T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(And(listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("andT2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<T>) =
    Deferred<List<T>>(And(listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("andT3")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(And(listOf(this.step, other.step)), this.workflowTaskContext)

// extension function to apply AND to a List<Deferred<T>>
fun <T> List<Deferred<T>>.and() =
    Deferred<List<T>>(And(this.map { it.step }), this.first().workflowTaskContext)
