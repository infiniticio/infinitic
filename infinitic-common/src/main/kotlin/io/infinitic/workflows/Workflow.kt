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

interface Workflow {
    /*
     *  Proxy task (Java syntax)
     */
    fun <T : Any> task(klass: Class<out T>): T

    /*
    *  Proxy workflow (Java syntax)
    */
    fun <T : Workflow> workflow(klass: Class<out T>): T

    /*
     *  Dispatch a task asynchronously
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S>

    /*
     * Dispatch a workflow asynchronously
     */
    fun <T : Workflow, S> async(proxy: T, method: T.() -> S): Deferred<S>

    /*
     * Create an async branch
     */
    fun <S> async(branch: () -> S): Deferred<S>

    /*
     * Create an inline task
     */
    fun <S> inline(task: () -> S): S
}

/*
 * Proxy task (preferred Kotlin syntax)
 */
inline fun <reified T : Any> Workflow.task() = this.task(T::class.java)

/*
 * Proxy workflow (preferred Kotlin syntax)
 */
inline fun <reified T : Workflow> Workflow.workflow() = this.workflow(T::class.java)
