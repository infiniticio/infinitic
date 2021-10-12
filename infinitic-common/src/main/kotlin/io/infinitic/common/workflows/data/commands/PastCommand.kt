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

import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.workflows.WorkflowChangeCheckMode
import io.infinitic.common.workflows.data.workflows.WorkflowChangeCheckMode.ALL
import io.infinitic.common.workflows.data.workflows.WorkflowChangeCheckMode.NONE
import io.infinitic.common.workflows.data.workflows.WorkflowChangeCheckMode.SIMPLE_NAME_ONLY
import kotlinx.serialization.Serializable

@Serializable
data class PastCommand(
    val commandPosition: MethodRunPosition,
    val commandType: CommandType,
    val commandId: CommandId,
    val commandHash: CommandHash,
    val commandName: CommandName?,
    val commandSimpleName: CommandSimpleName,
    var commandStatus: CommandStatus
) {
    fun isTerminated() = commandStatus.isTerminated()

    fun isSameThan(newCommand: NewCommand, mode: WorkflowChangeCheckMode): Boolean =
        newCommand.commandPosition == commandPosition &&
            when (mode) {
                NONE ->
                    true
                SIMPLE_NAME_ONLY ->
                    newCommand.commandType == commandType && newCommand.commandSimpleName == commandSimpleName
                ALL ->
                    newCommand.commandHash == commandHash
            }
}
