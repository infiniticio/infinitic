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

@Suppress("unused", "MemberVisibilityCanBePrivate")
interface Deferred<T> {
    val step: Step
    val deferredHandler: DeferredHandler

    /*
     * Use this method to wait the completion or cancellation of a deferred and get its result
     */
    fun await(): T = deferredHandler.await(this)

    /*
     * Use this method to get the status of a deferred
     */
    fun status(): DeferredStatus = deferredHandler.status(this)

    fun isCompleted() = status() == DeferredStatus.COMPLETED

    fun isCanceled() = status() == DeferredStatus.CANCELED

    fun isOngoing() = status() == DeferredStatus.ONGOING
}

@JvmName("or0")
infix fun <T> Deferred<out T>.or(other: Deferred<out T>) = this.deferredHandler.or0(this, other)

@JvmName("or1")
infix fun <T> Deferred<List<T>>.or(other: Deferred<out T>) = this.deferredHandler.or1(this, other)

@JvmName("or2")
infix fun <T> Deferred<List<T>>.or(other: Deferred<List<T>>) = this.deferredHandler.or2(this, other)

@JvmName("or3")
infix fun <T> Deferred<out T>.or(other: Deferred<List<T>>) = this.deferredHandler.or3(this, other)

@JvmName("and0")
infix fun <T> Deferred<out T>.and(other: Deferred<out T>) = this.deferredHandler.and0(this, other)

@JvmName("and1")
infix fun <T> Deferred<List<T>>.and(other: Deferred<out T>) = this.deferredHandler.and1(this, other)

@JvmName("and2")
infix fun <T> Deferred<List<T>>.and(other: Deferred<List<T>>) = this.deferredHandler.and2(this, other)

@JvmName("and3")
infix fun <T> Deferred<out T>.and(other: Deferred<List<T>>) = this.deferredHandler.and3(this, other)
