package io.infinitic.common.workflowManager

import io.infinitic.common.workflowManager.data.steps.Step
import io.infinitic.common.workflowManager.data.steps.StepStatus
import io.infinitic.common.workflowManager.data.steps.Step.Or
import io.infinitic.common.workflowManager.data.steps.Step.And

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
    Deferred<T>(Or(kotlin.collections.listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("orT1")
infix fun <T> Deferred<List<T>>.or(other: Deferred<T>) =
    Deferred<Any>(Or(kotlin.collections.listOf(this.step, other.step)), this.workflowTaskContext)

// extension function to apply OR to a List<Deferred<T>>
fun <T> List<Deferred<T>>.or() =
    Deferred<T>(Or(this.map { it.step }), this.first().workflowTaskContext)

@JvmName("andT0")
infix fun <T> Deferred<T>.and(other: Deferred<T>) =
    Deferred<List<T>>(And(kotlin.collections.listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("andT1")
infix fun <T> Deferred<T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(And(kotlin.collections.listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("andT2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<T>) =
    Deferred<List<T>>(And(kotlin.collections.listOf(this.step, other.step)), this.workflowTaskContext)

@JvmName("andT3")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(And(kotlin.collections.listOf(this.step, other.step)), this.workflowTaskContext)

// extension function to apply AND to a List<Deferred<T>>
fun <T> List<Deferred<T>>.and() =
    Deferred<List<T>>(And(this.map { it.step }), this.first().workflowTaskContext)
