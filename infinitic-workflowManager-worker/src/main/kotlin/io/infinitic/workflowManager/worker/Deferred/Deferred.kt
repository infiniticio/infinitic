package io.infinitic.workflowManager.worker.Deferred

import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.worker.data.MethodContext

class Deferred<out T>(val step : Step) {

    fun await() =

    fun result() = null  as T

    fun status() : Status = Status.CANCELED
}

// infix functions du compose Deferred
infix fun <T> Deferred<T>.or(other: Deferred<T>)= Deferred<T>(Step.Or(listOf(this.step, other.step)))
@JvmName("andT0") infix fun <T> Deferred<T>.and(other: Deferred<T>) = Deferred<List<T>>(Step.And(listOf(this.step, other.step)))
@JvmName("andT1") infix fun <T> Deferred<T>.and(other: Deferred<List<T>>) = Deferred<List<T>>(Step.And(listOf(this.step, other.step)))
@JvmName("andT2") infix fun <T> Deferred<List<T>>.and(other: Deferred<T>) = Deferred<List<T>>(Step.And(listOf(this.step, other.step)))
@JvmName("andT3") infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) = Deferred<List<T>>(Step.And(listOf(this.step, other.step)))

