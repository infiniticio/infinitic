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
package io.infinitic.workflows.engine.commands

import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.StartDurationTimerCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.state.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.startDurationTimerCmd(
  pastCommand: StartDurationTimerPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer
) {
  val command: StartDurationTimerCommand = pastCommand.command

  val msg = TimerCompleted(
      workflowName = state.workflowName,
      workflowId = state.workflowId,
      methodRunId = state.runningMethodRunId ?: thisShouldNotHappen(),
      timerId = TimerId.from(pastCommand.commandId),
      emitterName = ClientName(producer.name),
  )
  // The duration is offset by the time spent in the workflow task
  val diff = state.runningWorkflowTaskInstant!! - MillisInstant.now()

  launch { producer.send(msg, command.duration + diff) }
}
