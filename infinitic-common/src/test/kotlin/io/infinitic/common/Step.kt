package io.infinitic.common

import io.infinitic.workflows.or
import java.time.Instant

sealed class Step {
    data class Id(
        var status: Boolean
    ) : Step()

    data class And(val steps: List<Step>) : Step()

    data class Or(val steps: List<Step>) : Step()
}

fun and(step1: Step.Id, step2: Step.Id) = Step.And(listOf(step1, step2))
fun and(step1: Step.Id, step2: Step.And) = Step.And(listOf(step1) + step2.steps)
fun and(step1: Step.Id, step2: Step.Or) = Step.And(listOf(step1, step2))
fun and(step1: Step.And, step2: Step.Id) = Step.And(step1.steps + step2)
fun and(step1: Step.And, step2: Step.And) = Step.And(step1.steps + step2.steps)
fun and(step1: Step.And, step2: Step.Or) = Step.And(listOf(step1, step2))
fun and(step1: Step.Or, step2: Step.Id) = Step.And(listOf(step1, step2))
fun and(step1: Step.Or, step2: Step.And) = Step.And(listOf(step1, step2))
fun and(step1: Step.Or, step2: Step.Or) = Step.And(listOf(step1, step2))

fun or(step1: Step.Id, step2: Step.Id) = Step.Or(listOf(step1, step2))
fun or(step1: Step.Id, step2: Step.And) = Step.Or(listOf(step1, step2))
fun or(step1: Step.Id, step2: Step.Or) = Step.Or(listOf(step1) + step2)
fun or(step1: Step.And, step2: Step.Id) = Step.Or(listOf(step1, step2))
fun or(step1: Step.And, step2: Step.And) = Step.Or(listOf(step1, step2))
fun or(step1: Step.And, step2: Step.Or) = Step.Or(listOf(step1, step2))
fun or(step1: Step.Or, step2: Step.Id) = Step.Or(step1.steps + step2)
fun or(step1: Step.Or, step2: Step.And) = Step.Or(listOf(step1, step2))
fun or(step1: Step.Or, step2: Step.Or) = Step.Or(step1.steps + step2.steps)

fun or(step1: Step, step2: Step) = when (step1) {
    is Step.And -> when (step2) {
        is Step.And -> or(step1, step2)
        is Step.Id -> or(step1, step2)
        is Step.Or -> or(step1, step2)
    }
    is Step.Id -> when (step2) {
        is Step.And -> or(step1, step2)
        is Step.Id -> or(step1, step2)
        is Step.Or -> or(step1, step2)
    }
    is Step.Or -> when (step2) {
        is Step.And -> or(step1, step2)
        is Step.Id -> or(step1, step2)
        is Step.Or -> or(step1, step2)
    }
}

fun and(step1: Step, step2: Step) = when (step1) {
    is Step.And -> when (step2) {
        is Step.And -> and(step1, step2)
        is Step.Id -> and(step1, step2)
        is Step.Or -> and(step1, step2)
    }
    is Step.Id -> when (step2) {
        is Step.And -> and(step1, step2)
        is Step.Id -> and(step1, step2)
        is Step.Or -> and(step1, step2)
    }
    is Step.Or -> when (step2) {
        is Step.And -> and(step1, step2)
        is Step.Id -> and(step1, step2)
        is Step.Or -> and(step1, step2)
    }
}

val i1 = Step.Id(true)
val i2 = Step.Id(true)
val i3 = Step.Id(true)
val i4 = Step.Id(true)

val and1: Step.And = and(i4, and(i1, i2))
val and2: Step.And = and(i1, i3)

val or1: Step.Or = or(i1, i2)
val or2: Step.Or = or(i1, i3)

data class Deferred<T> (
    val step: Step
)

@JvmName("orT0")
infix fun <T> Deferred<out T>.or(other: Deferred<out T>) =
    Deferred<T>(or(this.step, other.step))

@JvmName("orT1")
infix fun <T> Deferred<List<T>>.or(other: Deferred<out T>) =
    Deferred<Any>(or(this.step, other.step))

@JvmName("orT2")
infix fun <T> Deferred<List<T>>.or(other: Deferred<List<T>>) =
    Deferred<List<T>>(or(this.step, other.step))

@JvmName("orT3")
infix fun <T> Deferred<out T>.or(other: Deferred<List<T>>) =
    Deferred<Any>(or(this.step, other.step))

@JvmName("andT0")
infix fun <T> Deferred<out T>.and(other: Deferred<out T>) =
    Deferred<List<T>>(and(this.step, other.step))

@JvmName("andT1")
infix fun <T> Deferred<List<T>>.and(other: Deferred<out T>) =
    Deferred<List<T>>(and(this.step, other.step))

@JvmName("andT2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(and(this.step, other.step))

@JvmName("andT3")
infix fun <T> Deferred<out T>.and(other: Deferred<List<T>>) =
    Deferred<List<T>>(and(this.step, other.step))

val d1: Deferred<String> = Deferred(i1)
val d2: Deferred<String> = Deferred(i2)
val d3: Deferred<Int> = Deferred(i3)
val d4: Deferred<Instant> = Deferred(i4)

val r1: Deferred<out Any> = d1 or d2 or d4
val r2: Deferred<List<Any>> = d1 and d3
