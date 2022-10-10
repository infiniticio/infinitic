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
    val context: ThreadLocal<TaskContext> = ThreadLocal.withInitial { null }

    @JvmStatic
    val workerName get() = context.get().workerName

    @JvmStatic
    val serviceName get() = context.get().serviceName

    @JvmStatic
    val taskId get() = context.get().taskId

    @JvmStatic
    val taskName get() = context.get().taskName

    @JvmStatic
    val workflowId get() = context.get().workflowId

    @JvmStatic
    val workflowName get() = context.get().workflowName

    @JvmStatic
    val retrySequence get() = context.get().retrySequence

    @JvmStatic
    val lastError get() = context.get().lastError

    @JvmStatic
    val retryIndex get() = context.get().retryIndex

    @JvmStatic
    val tags get() = context.get().tags

    @JvmStatic
    val meta get() = context.get().meta

    @JvmStatic
    val options get() = context.get().options

    @JvmStatic
    val registry get() = context.get().workerRegistry

    @JvmStatic
    val client get() = context.get().client
}
