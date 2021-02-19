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
import kotlinx.serialization.Serializable

@Serializable
data class NewCommand(
    val commandId: CommandId = CommandId(),
    val command: Command,
    val commandSimpleName: CommandSimpleName,
    val commandPosition: MethodRunPosition
) {
    val commandStatus: CommandStatus = CommandStatusOngoing

    val commandHash: CommandHash = command.hash()

    val commandType: CommandType = when (command) {
        is DispatchTask -> CommandType.DISPATCH_TASK
        is DispatchChildWorkflow -> CommandType.DISPATCH_CHILD_WORKFLOW
        is DispatchDurationTimer -> CommandType.DISPATCH_DURATION_TIMER
        is DispatchInstantTimer -> CommandType.DISPATCH_INSTANT_TIMER
        is StartAsync -> CommandType.START_ASYNC
        is EndAsync -> CommandType.END_ASYNC
        is StartInlineTask -> CommandType.START_INLINE_TASK
        is EndInlineTask -> CommandType.END_INLINE_TASK
    }
}
