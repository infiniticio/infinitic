package io.infinitic.workflowManager.worker.deferred

import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.exceptions.MixingDeferredFromDifferentWorkflowMethodExecution
import io.infinitic.workflowManager.worker.data.MethodExecutionContext

data class Deferred<out T>(val step : Step, internal val methodExecutionContext: MethodExecutionContext) {

    fun await() : Deferred<T> = methodExecutionContext.await(this)

    fun result() : T = methodExecutionContext.result(this)

    fun status() : DeferredStatus = methodExecutionContext.status(this)
}

// infix functions to compose Deferred
infix fun <T> Deferred<T>.or(other: Deferred<T>)=
    Deferred<T>(Step.Or(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT0") infix fun <T> Deferred<T>.and(other: Deferred<T>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT1") infix fun <T> Deferred<T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT2") infix fun <T> Deferred<List<T>>.and(other: Deferred<T>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT3") infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

private fun <T> getMethodExecutionContext(d1: Deferred<T>, d2: Deferred<T>): MethodExecutionContext {
    if (d1.methodExecutionContext != d2.methodExecutionContext) throw MixingDeferredFromDifferentWorkflowMethodExecution()

    return d1.methodExecutionContext
}
