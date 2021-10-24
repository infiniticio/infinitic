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

package io.infinitic.common.workflows.data.commands

import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.Serializable
import java.util.UUID

@JvmInline @Serializable
value class CommandId(private val id: String = UUID.randomUUID().toString()) {
    companion object {
        fun from(taskId: TaskId) = CommandId(taskId.toString())
        fun from(workflowId: WorkflowId) = CommandId(workflowId.toString())
        fun from(methodRunId: MethodRunId) = CommandId(methodRunId.toString())
        fun from(timerId: TimerId) = CommandId(timerId.toString())
    }

    override fun toString() = id
}
