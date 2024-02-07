/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
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
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.workflows.WorkflowCheckMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PastCommand {
  abstract val commandId: CommandId
  abstract val commandPosition: PositionInWorkflowMethod
  abstract val commandSimpleName: CommandSimpleName
  abstract var commandStatus: CommandStatus
  abstract val command: Command

  companion object {
    fun from(
      commandId: CommandId,
      commandPosition: PositionInWorkflowMethod,
      commandSimpleName: CommandSimpleName,
      commandStatus: CommandStatus,
      command: Command
    ) = when (command) {
      is DispatchNewMethodCommand -> DispatchNewMethodPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is DispatchTaskCommand -> DispatchTaskPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is DispatchNewWorkflowCommand -> DispatchNewWorkflowPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is InlineTaskCommand -> InlineTaskPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is ReceiveSignalCommand -> ReceiveSignalPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is SendSignalCommand -> SendSignalPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is StartDurationTimerCommand -> StartDurationTimerPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )

      is StartInstantTimerCommand -> StartInstantTimerPastCommand(
          commandId, commandPosition, commandSimpleName, commandStatus, command,
      )
    }
  }

  open fun setTerminatedStatus(commandStatus: CommandStatus) {
    this.commandStatus = commandStatus
  }

  open fun isTerminated() = commandStatus.isTerminated()

  fun isSameThan(other: PastCommand, mode: WorkflowCheckMode): Boolean =
      other.commandPosition == commandPosition && when (mode) {
        WorkflowCheckMode.none -> true
        WorkflowCheckMode.simple ->
          (other.command::class == command::class && other.commandSimpleName == commandSimpleName)

        WorkflowCheckMode.strict -> command.isSameThan(other.command)
      }
}

@Serializable
@SerialName("PastCommand.DispatchTask")
data class DispatchTaskPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: DispatchTaskCommand,
  var taskRetrySequence: TaskRetrySequence = TaskRetrySequence(0),
) : PastCommand()

@Serializable
@SerialName("PastCommand.DispatchWorkflow")
data class DispatchNewWorkflowPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: DispatchNewWorkflowCommand
) : PastCommand()

@Serializable
@SerialName("PastCommand.DispatchMethod")
data class DispatchNewMethodPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: DispatchNewMethodCommand
) : PastCommand()

@Serializable
@SerialName("PastCommand.InlineTask")
data class InlineTaskPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: InlineTaskCommand
) : PastCommand()

@Serializable
@SerialName("PastCommand.ReceiveSignal")
data class ReceiveSignalPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: ReceiveSignalCommand,
  @AvroDefault("[]") val commandStatuses: MutableList<CommandStatus> = mutableListOf()
) : PastCommand() {
  override fun setTerminatedStatus(commandStatus: CommandStatus) {
    when (commandStatus) {
      is CommandStatus.Completed ->
        when (commandStatus.returnIndex) {
          0 -> this.commandStatus = commandStatus
          else -> commandStatuses.add(commandStatus)
        }

      else -> this.commandStatus = commandStatus
    }
  }

  // Per default this command is never terminated
  override fun isTerminated() = false
}

@Serializable
@SerialName("PastCommand.SendSignal")
data class SendSignalPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: SendSignalCommand
) : PastCommand()

@Serializable
@SerialName("PastCommand.StartDurationTimer")
data class StartDurationTimerPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: StartDurationTimerCommand
) : PastCommand()

@Serializable
@SerialName("PastCommand.StartInstantTimer")
data class StartInstantTimerPastCommand(
  override val commandId: CommandId,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val command: StartInstantTimerCommand
) : PastCommand()
