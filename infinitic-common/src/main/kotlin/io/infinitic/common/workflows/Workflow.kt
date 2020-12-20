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

package io.infinitic.common.workflows

import io.infinitic.common.workflows.executors.proxies.TaskProxyHandler
import io.infinitic.common.workflows.executors.proxies.WorkflowProxyHandler
import kotlin.reflect.KClass

interface Workflow {
    var context: WorkflowTaskContext
}

/*
 * Proxy a task
 */
inline fun <reified T : Any> Workflow.task() = TaskProxyHandler(T::class.java) { context }.instance()
fun <T : Any> Workflow.proxy(klass: Class<out T>) = TaskProxyHandler(klass) { context }.instance()
fun <T : Any> Workflow.proxy(klass: KClass<out T>) = TaskProxyHandler(klass.java) { context }.instance()

/*
 * Proxy a child workflow
 */
inline fun <reified T : Workflow> Workflow.workflow() = WorkflowProxyHandler(T::class.java) { context }.instance()
fun <T : Workflow> Workflow.proxy(klass: Class<out T>) = WorkflowProxyHandler(klass) { context }.instance()
fun <T : Workflow> Workflow.proxy(klass: KClass<out T>) = WorkflowProxyHandler(klass.java) { context }.instance()

/*
 * Dispatch
 */
fun <T : Any, S> Workflow.async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

/*
 * Dispatch a workflow
 */
fun <T : Workflow, S> Workflow.async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

/*
 * Create an async branch
 */
fun <S> Workflow.async(branch: () -> S) = context.async(branch)

/*
 * Create an inline task
 */
fun <S> Workflow.task(inline: () -> S): S = context.task(inline)
