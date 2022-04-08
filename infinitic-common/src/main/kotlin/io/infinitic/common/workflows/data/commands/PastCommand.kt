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

import com.github.avrokotlin.avro4k.AvroDefault
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.workflows.WorkflowChangeCheckMode
import io.infinitic.workflows.WorkflowChangeCheckMode.NONE
import io.infinitic.workflows.WorkflowChangeCheckMode.SIMPLE
import io.infinitic.workflows.WorkflowChangeCheckMode.STRICT
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PastCommand {
    abstract val commandId: CommandId
    abstract val commandPosition: MethodRunPosition
    abstract val commandSimpleName: CommandSimpleName
    abstract var commandStatus: CommandStatus
    abstract val command: Command

    companion object {
        fun from(
            commandPosition: MethodRunPosition,
            commandSimpleName: CommandSimpleName,
            commandStatus: CommandStatus,
            command: Command
        ) = when (command) {
            is DispatchMethodCommand -> {
                DispatchMethodPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command)
            }
            is DispatchTaskCommand -> {
                DispatchTaskPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command, TaskRetrySequence(0))
            }
            is DispatchWorkflowCommand -> {
                DispatchWorkflowPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command)
            }
            is InlineTaskCommand -> {
                InlineTaskPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command)
            }
            is ReceiveSignalCommand -> {
                ReceiveSignalPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, listOf(), command)
            }
            is SendSignalCommand -> {
                SendSignalPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command)
            }
            is StartDurationTimerCommand -> {
                StartDurationTimerPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command)
            }
            is StartInstantTimerCommand -> {
                StartInstantTimerPastCommand(CommandId(), commandPosition, commandSimpleName, commandStatus, command)
            }
        }
    }
    fun isTerminated() = commandStatus.isTerminated()

    fun isSameThan(other: PastCommand, mode: WorkflowChangeCheckMode): Boolean =
        other.commandPosition == commandPosition &&
            when (mode) {
                NONE ->
                    true
                SIMPLE ->
                    other.command::class == command::class && other.commandSimpleName == commandSimpleName
                STRICT ->
                    command.isSameThan(other.command)
            }
}

@Serializable @SerialName("PastCommand.DispatchTask")
data class DispatchTaskPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: DispatchTaskCommand,
    var taskRetrySequence: TaskRetrySequence
) : PastCommand()

@Serializable @SerialName("PastCommand.DispatchWorkflow")
data class DispatchWorkflowPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: DispatchWorkflowCommand
) : PastCommand()

@Serializable @SerialName("PastCommand.DispatchMethod")
data class DispatchMethodPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: DispatchMethodCommand
) : PastCommand()

@Serializable @SerialName("PastCommand.InlineTask")
data class InlineTaskPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: InlineTaskCommand
) : PastCommand()

@Serializable @SerialName("PastCommand.ReceiveSignal")
data class ReceiveSignalPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    @AvroDefault("[]")
    var commandStatuses: List<CommandStatus>,
    override val command: ReceiveSignalCommand
) : PastCommand()

@Serializable @SerialName("PastCommand.SendSignal")
data class SendSignalPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: SendSignalCommand
) : PastCommand()

@Serializable @SerialName("PastCommand.StartDurationTimer")
data class StartDurationTimerPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: StartDurationTimerCommand
) : PastCommand()

@Serializable @SerialName("PastCommand.StartInstantTimer")
data class StartInstantTimerPastCommand(
    override val commandId: CommandId,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val command: StartInstantTimerCommand
) : PastCommand()
