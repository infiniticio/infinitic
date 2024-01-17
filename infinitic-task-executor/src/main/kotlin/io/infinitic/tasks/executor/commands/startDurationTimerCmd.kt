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
package io.infinitic.tasks.executor.commands

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.StartDurationTimerCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.tasks.executor.TaskEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.startDurationTimerCmd(
  currentWorkflow: TaskEventHandler.CurrentWorkflow,
  pastCommand: StartDurationTimerPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer
) {
  val emitterName = EmitterName(producer.name)
  val command: StartDurationTimerCommand = pastCommand.command

  val timerCompleted = TimerCompleted(
      timerId = TimerId.from(pastCommand.commandId),
      workflowName = currentWorkflow.workflowName,
      workflowId = currentWorkflow.workflowId,
      workflowMethodId = currentWorkflow.workflowMethodId,
      emitterName = emitterName,
      emittedAt = workflowTaskInstant + command.duration,
  )

  // The duration is offset by the time spent in the workflow task
  // todo: Check if there is a way not to use MillisInstant.now()
  launch {
    producer.sendToWorkflowEngineAfter(
        timerCompleted,
        workflowTaskInstant + command.duration - MillisInstant.now(),
    )
  }
}
