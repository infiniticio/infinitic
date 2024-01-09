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
import io.infinitic.common.workflows.data.methodRuns.PositionInWorkflowMethod
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
      command: Command,
      commandPosition: PositionInWorkflowMethod,
      commandSimpleName: CommandSimpleName,
      commandStatus: CommandStatus
    ) =
        when (command) {
          is DispatchMethodOnRunningWorkflowCommand -> {
            DispatchMethodOnRunningWorkflowPastCommand(
                command,
                commandPosition,
                commandSimpleName,
                commandStatus,
            )
          }

          is DispatchTaskCommand -> {
            DispatchTaskPastCommand(command, commandPosition, commandSimpleName, commandStatus)
          }

          is DispatchNewWorkflowCommand -> {
            DispatchNewWorkflowPastCommand(
                command,
                commandPosition,
                commandSimpleName,
                commandStatus,
            )
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
            StartDurationTimerPastCommand(
                command, commandPosition, commandSimpleName, commandStatus,
            )
          }

          is StartInstantTimerCommand -> {
            StartInstantTimerPastCommand(command, commandPosition, commandSimpleName, commandStatus)
          }
        }
  }

  open fun setTerminatedStatus(commandStatus: CommandStatus) {
    this.commandStatus = commandStatus
  }

  open fun isTerminated() = commandStatus.isTerminated()

  fun isSameThan(other: PastCommand, mode: WorkflowCheckMode): Boolean =
      other.commandPosition == commandPosition &&
          when (mode) {
            WorkflowCheckMode.none -> true
            WorkflowCheckMode.simple ->
              other.command::class == command::class &&
                  other.commandSimpleName == commandSimpleName

            WorkflowCheckMode.strict -> command.isSameThan(other.command)
          }
}

@Serializable
@SerialName("PastCommand.DispatchTask")
data class DispatchTaskPastCommand(
  override val command: DispatchTaskCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  var taskRetrySequence: TaskRetrySequence = TaskRetrySequence(0),
  override val commandId: CommandId = CommandId()
) : PastCommand()

@Serializable
@SerialName("PastCommand.DispatchWorkflow")
data class DispatchNewWorkflowPastCommand(
  override val command: DispatchNewWorkflowCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId()
) : PastCommand()

@Serializable
@SerialName("PastCommand.DispatchMethod")
data class DispatchMethodOnRunningWorkflowPastCommand(
  override val command: DispatchMethodOnRunningWorkflowCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId()
) : PastCommand()

@Serializable
@SerialName("PastCommand.InlineTask")
data class InlineTaskPastCommand(
  override val command: InlineTaskCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId()
) : PastCommand()

@Serializable
@SerialName("PastCommand.ReceiveSignal")
data class ReceiveSignalPastCommand(
  override val command: ReceiveSignalCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId(),
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
  override val command: SendSignalCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId()
) : PastCommand()

@Serializable
@SerialName("PastCommand.StartDurationTimer")
data class StartDurationTimerPastCommand(
  override val command: StartDurationTimerCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId()
) : PastCommand()

@Serializable
@SerialName("PastCommand.StartInstantTimer")
data class StartInstantTimerPastCommand(
  override val command: StartInstantTimerCommand,
  override val commandPosition: PositionInWorkflowMethod,
  override val commandSimpleName: CommandSimpleName,
  override var commandStatus: CommandStatus,
  override val commandId: CommandId = CommandId()
) : PastCommand()
