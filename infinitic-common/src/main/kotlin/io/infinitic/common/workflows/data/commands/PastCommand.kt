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
import io.infinitic.common.exceptions.thisShouldNotHappen
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
            command: Command,
            commandPosition: MethodRunPosition,
            commandSimpleName: CommandSimpleName,
            commandStatus: CommandStatus
        ) = when (command) {
            is DispatchMethodCommand -> {
                DispatchMethodPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is DispatchTaskCommand -> {
                DispatchTaskPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is DispatchWorkflowCommand -> {
                DispatchWorkflowPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is InlineTaskCommand -> {
                InlineTaskPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is ReceiveSignalCommand -> {
                ReceiveSignalPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is SendSignalCommand -> {
                SendSignalPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is StartDurationTimerCommand -> {
                StartDurationTimerPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
            is StartInstantTimerCommand -> {
                StartInstantTimerPastCommand(command, commandPosition, commandSimpleName, commandStatus)
            }
        }
    }

    fun setStatus(commandStatus: CommandStatus) {
        when (this) {
            is ReceiveSignalPastCommand -> when (this.commandStatus) {
                CommandStatus.Ongoing -> this.commandStatus = commandStatus
                else -> when {
                    command.channelSignalLimit == null -> commandStatuses.add(commandStatus)
                    command.channelSignalLimit > commandStatuses.size + 1 -> commandStatuses.add(commandStatus)
                    else -> thisShouldNotHappen()
                }
            }
            else -> {
                this.commandStatus = commandStatus
            }
        }
    }

    fun isTerminated() = when (this) {
        is ReceiveSignalPastCommand -> when (command.channelSignalLimit) {
            null -> false
            else -> commandStatus.isTerminated() &&
                commandStatuses.all { it.isTerminated() } &&
                command.channelSignalLimit == (commandStatuses.size + 1)
        }
        else -> commandStatus.isTerminated()
    }

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
    override val command: DispatchTaskCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    var taskRetrySequence: TaskRetrySequence = TaskRetrySequence(0),
    override val commandId: CommandId = CommandId(),
) : PastCommand()

@Serializable @SerialName("PastCommand.DispatchWorkflow")
data class DispatchWorkflowPastCommand(
    override val command: DispatchWorkflowCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
) : PastCommand()

@Serializable @SerialName("PastCommand.DispatchMethod")
data class DispatchMethodPastCommand(
    override val command: DispatchMethodCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
) : PastCommand()

@Serializable @SerialName("PastCommand.InlineTask")
data class InlineTaskPastCommand(
    override val command: InlineTaskCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
) : PastCommand()

@Serializable @SerialName("PastCommand.ReceiveSignal")
data class ReceiveSignalPastCommand(
    override val command: ReceiveSignalCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
    @AvroDefault("[]")
    val commandStatuses: MutableList<CommandStatus> = mutableListOf(),
) : PastCommand()

@Serializable @SerialName("PastCommand.SendSignal")
data class SendSignalPastCommand(
    override val command: SendSignalCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
) : PastCommand()

@Serializable @SerialName("PastCommand.StartDurationTimer")
data class StartDurationTimerPastCommand(
    override val command: StartDurationTimerCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
) : PastCommand()

@Serializable @SerialName("PastCommand.StartInstantTimer")
data class StartInstantTimerPastCommand(
    override val command: StartInstantTimerCommand,
    override val commandPosition: MethodRunPosition,
    override val commandSimpleName: CommandSimpleName,
    override var commandStatus: CommandStatus,
    override val commandId: CommandId = CommandId(),
) : PastCommand()
