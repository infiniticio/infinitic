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

import io.infinitic.common.workflows.executors.proxies.TaskProxyHandler
import io.infinitic.common.workflows.executors.proxies.WorkflowProxyHandler

abstract class Workflow {
    lateinit var context: WorkflowTaskContext

    /*
     *  Stub task
     */
    fun <T : Any> task(klass: Class<out T>): T = TaskProxyHandler(klass) { context }.instance()

    /*
     * Stub task
     * (Kotlin way)
     */
    inline fun <reified T : Any> task(): T = TaskProxyHandler(T::class.java) { context }.instance()

    /*
     *  Stub workflow
     */
    fun <T : Any> workflow(klass: Class<out T>): T = WorkflowProxyHandler(klass) { context }.instance()

    /*
     *  Stub workflow
     * (Kotlin way)
     */
    inline fun <reified T : Any> workflow(): T = WorkflowProxyHandler(T::class.java) { context }.instance()

    /*
     *  Dispatch a task or a workflow asynchronously
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

    /*
     * Create an async branch
     */
    fun <S> async(branch: () -> S): Deferred<S> = context.async(branch)

    /*
     * Create an inline task
     */
    fun <S> inline(task: () -> S): S = context.inline(task)
}
