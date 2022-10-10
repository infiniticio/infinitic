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

package io.infinitic.tasks

object Task {
    @JvmStatic
    val context: ThreadLocal<TaskContext> = ThreadLocal.withInitial { null }

    val workerName
        get() = context.get().workerName
    val taskId
        get() = context.get().taskId

    val workflowId
        get() = context.get().workflowId

    val workflowName
        get() = context.get().workflowName
    val retryIndex
        get() = context.get().retryIndex

    val retrySequence
        get() = context.get().retrySequence

    val tags
        get() = context.get().tags

    val meta
        get() = context.get().meta
    val registry
        get() = context.get().workerRegistry

    val client
        get() = context.get().client
}
