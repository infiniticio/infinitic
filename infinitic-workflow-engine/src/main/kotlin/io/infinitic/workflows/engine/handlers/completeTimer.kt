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

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerPastCommand
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.state.WorkflowState

internal fun completeTimer(state: WorkflowState, message: CompleteTimers) {

  fun WorkflowMethod.completeTimer() {
    pastCommands
        .filter { it is StartDurationTimerPastCommand || it is StartInstantTimerPastCommand }
        .filter { it.commandStatus is CommandStatus.Ongoing }
        .sortedByDescending { it.commandPosition }
        .forEach {
          val msg = RemoteTimerCompleted(
              timerId = TimerId.from(it.commandId),
              workflowName = state.workflowName,
              workflowId = state.workflowId,
              workflowMethodName = methodName,
              workflowMethodId = workflowMethodId,
              emitterName = message.emitterName,
              emittedAt = message.emittedAt ?: thisShouldNotHappen(),
          )
          // add fake message at the top of the messagesBuffer list
          state.messagesBuffer.add(0, msg)
        }
  }

  // get provided workflowMethodId or all by default
  val workflowMethodIds = message.workflowMethodId?.let {
    listOf(it)
  } ?: state.workflowMethods.map { it.workflowMethodId }

  // trigger a timer completed for all ongoing timer on those methods
  workflowMethodIds.forEach {
    state.getWorkflowMethod(it)?.completeTimer()
  }
}
