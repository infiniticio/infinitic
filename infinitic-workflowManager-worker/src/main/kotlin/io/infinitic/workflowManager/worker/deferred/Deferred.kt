package io.infinitic.workflowManager.worker.deferred

import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.exceptions.MixingDeferredFromDifferentWorkflowMethodExecution
import io.infinitic.workflowManager.worker.data.MethodRunContext

data class Deferred<out T>(
    internal val step: Step,
    internal val methodRunContext: MethodRunContext
) {

    fun await(): Deferred<T> = methodRunContext.await(this)

    fun result(): T = methodRunContext.result(this)

    fun status(): DeferredStatus = methodRunContext.status(this)
}

// infix functions to compose Deferred
infix fun <T> Deferred<T>.or(other: Deferred<T>) =
    Deferred<T>(Step.Or(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT0") infix fun <T> Deferred<T>.and(other: Deferred<T>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT1") infix fun <T> Deferred<T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT2") infix fun <T> Deferred<List<T>>.and(other: Deferred<T>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))
@JvmName("andT3") infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(Step.And(listOf(this.step, other.step)), getMethodExecutionContext(this, other))

private fun <T> getMethodExecutionContext(d1: Deferred<T>, d2: Deferred<T>): MethodRunContext {
    if (d1.methodRunContext != d2.methodRunContext) throw MixingDeferredFromDifferentWorkflowMethodExecution()

    return d1.methodRunContext
}
