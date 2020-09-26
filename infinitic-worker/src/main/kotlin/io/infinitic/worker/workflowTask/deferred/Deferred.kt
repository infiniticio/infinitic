package io.infinitic.worker.workflowTask.deferred

import io.infinitic.common.workflowManager.data.steps.Step
import io.infinitic.common.workflowManager.data.steps.StepStatus
import io.infinitic.common.workflowManager.exceptions.MixingDeferredFromDifferentWorkflowMethodExecution
import io.infinitic.worker.workflowTask.WorkflowTaskContext

data class Deferred<out T>(
    internal val step: Step,
    internal val workflowTaskContext: WorkflowTaskContext
) {
    internal lateinit var stepStatus: StepStatus

    fun await(): Deferred<T> = workflowTaskContext.await(this)

    fun result(): T = workflowTaskContext.result(this)

    fun status(): DeferredStatus = workflowTaskContext.status(this)
}

// infix functions to compose Deferred
infix fun <T> Deferred<T>.or(other: Deferred<T>) =
    Deferred<T>(Step.Or(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

// extension function to apply OR to a List<Deferred<T>>
fun <T> List<Deferred<T>>.or() =
    Deferred<T>(Step.Or(this.map { it.step }), getMethodExecutionContext(this))

@JvmName("andT0")
infix fun <T> Deferred<T>.and(other: Deferred<T>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

@JvmName("andT1")
infix fun <T> Deferred<T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

@JvmName("andT2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<T>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

@JvmName("andT3")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

// extension function to apply AND to a List<Deferred<T>>
fun <T> List<Deferred<T>>.and() =
    Deferred<List<T>>(Step.And(this.map { it.step }), getMethodExecutionContext(this))

private fun <T> getMethodExecutionContext(d1: Deferred<T>, d2: Deferred<T>): WorkflowTaskContext {
    if (d1.workflowTaskContext != d2.workflowTaskContext) throw MixingDeferredFromDifferentWorkflowMethodExecution()

    return d1.workflowTaskContext
}

private fun <T> getMethodExecutionContext(list: List<Deferred<T>>): WorkflowTaskContext {
    val d = list.distinctBy { it.workflowTaskContext }
    if (d.size > 1) throw MixingDeferredFromDifferentWorkflowMethodExecution()

    return list.first().workflowTaskContext
}
