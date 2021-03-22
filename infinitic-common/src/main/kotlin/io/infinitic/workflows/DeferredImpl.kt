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

import io.infinitic.common.deferred.DeferredHandler
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatus

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class DeferredImpl<T> (
    override val step: Step,
    override val deferredHandler: DeferredHandler
) : Deferred<T> {

    lateinit var stepStatus: StepStatus
}

// fun or(vararg others: Deferred<*>) = others.reduce { acc, deferred -> acc or deferred }
//
// fun and(vararg others: Deferred<*>) = others.reduce { acc, deferred -> acc and deferred }
//
//
// @JvmName("or0")
// infix fun <T> Deferred<out T>.or(other: Deferred<out T>) = this.deferredHandler.or0(this, other)
//
// @JvmName("or1")
// infix fun <T> Deferred<List<T>>.or(other: Deferred<out T>) = this.deferredHandler.or1(this, other)
//
// @JvmName("or2")
// infix fun <T> Deferred<List<T>>.or(other: Deferred<List<T>>) = this.deferredHandler.or2(this, other)
//
// @JvmName("or3")
// infix fun <T> Deferred<out T>.or(other: Deferred<List<T>>) = this.deferredHandler.or3(this, other)
//
// @JvmName("and0")
// infix fun <T> Deferred<out T>.and(other: Deferred<out T>) = this.deferredHandler.and0(this, other)
//
// @JvmName("and1")
// infix fun <T> Deferred<List<T>>.and(other: Deferred<out T>) = this.deferredHandler.and1(this, other)
//
// @JvmName("and2")
// infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) = this.deferredHandler.and2(this, other)
//
// @JvmName("and3")
// infix fun <T> Deferred<out T>.and(other: Deferred<List<T>>) = this.deferredHandler.and3(this, other)
//
// // extension function to apply AND to a List<Deferred<T>>
// fun <T> List<Deferred<T>>.and() =
//    Deferred<List<T>>(Step.And(this.map { it.step }), this.first().deferredHandler)
//
// // extension function to apply OR to a List<Deferred<T>>
// fun <T> List<Deferred<T>>.or() =
//    Deferred<T>(Step.Or(this.map { it.step }), this.first().deferredHandler)
