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

package io.infinitic.common.deferred

import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus

@Suppress("unused", "MemberVisibilityCanBePrivate")
interface DeferredHandler {
    // wait deferred result
    fun <T> await(deferred: Deferred<T>): T

    // get deferred status
    fun <T> status(deferred: Deferred<T>): DeferredStatus

    // da or db
    fun <T> or0(d1: Deferred<out T>, d2: Deferred<out T>): Deferred<T>
    // (da and db) or dc
    fun <T> or1(d1: Deferred<List<T>>, d2: Deferred<out T>): Deferred<Any>
    // (da and db) or (dc and dd)
    fun <T> or2(d1: Deferred<List<T>>, d2: Deferred<List<T>>): Deferred<List<T>>
    // da or (db and dc)
    fun <T> or3(d1: Deferred<out T>, d2: Deferred<List<T>>): Deferred<Any>

    // da and db
    fun <T> and0(d1: Deferred<out T>, d2: Deferred<out T>): Deferred<List<T>>
    // (da and db) and dc
    fun <T> and1(d1: Deferred<List<T>>, d2: Deferred<out T>): Deferred<List<T>>
    // (da and db) and (dc and dd)
    fun <T> and2(d1: Deferred<List<T>>, d2: Deferred<List<T>>): Deferred<List<T>>
    // da and (db and dc)
    fun <T> and3(d1: Deferred<out T>, d2: Deferred<List<T>>): Deferred<List<T>>

    fun <T> or(list: List<Deferred<T>>): Deferred<T>
    fun <T> and(list: List<Deferred<T>>): Deferred<List<T>>

    fun <T> or(vararg array: Deferred<T>) = or(array.toList())
    fun <T> and(vararg array: Deferred<T>) = and(array.toList())
}
