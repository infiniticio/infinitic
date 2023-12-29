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
package io.infinitic.workflows.engine.handlers

import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerPastCommand
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.state.WorkflowState

internal fun completeTimer(state: WorkflowState, message: CompleteTimers) {
  // get provided methodRunId or main per default
  val methodRunId = message.workflowMethodId ?: WorkflowMethodId.from(message.workflowId)

  // trigger a timer completed for all ongoing timer on this method
  state.getMethodRun(methodRunId)?.let { methodRun ->
    methodRun.pastCommands
        .filter { it is StartDurationTimerPastCommand || it is StartInstantTimerPastCommand }
        .filter { it.commandStatus is CommandStatus.Ongoing }
        .sortedByDescending { it.commandPosition }
        .forEach {
          val msg =
              TimerCompleted(
                  TimerId.from(it.commandId),
                  message.workflowName,
                  message.workflowId,
                  methodRunId,
                  emitterName = message.emitterName,
              )
          // add fake message at the top of the messagesBuffer list
          state.messagesBuffer.add(0, msg)
        }
  }
}
