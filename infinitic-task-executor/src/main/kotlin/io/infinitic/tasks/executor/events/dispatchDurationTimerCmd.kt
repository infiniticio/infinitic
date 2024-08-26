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
package io.infinitic.tasks.executor.events

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.engine.commands.dispatchTimer
import io.infinitic.common.workflows.engine.messages.TimerDispatchedEvent
import io.infinitic.common.workflows.engine.messages.data.DurationTimerDispatched
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchDurationTimerCmd(
  current: WorkflowRequester,
  pastCommand: StartDurationTimerPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer
) = launch {
  val emitterName = EmitterName(producer.name)
  val startDurationTimer = pastCommand.command

  // Description of the dispatched timer
  val timerDispatched = DurationTimerDispatched(
      timerId = TimerId.from(pastCommand.commandId),
      duration = startDurationTimer.duration,
      emittedAt = workflowTaskInstant,
  )
  // Dispatching of the timer
  with(producer) { dispatchTimer(timerDispatched, current) }

  // Description of the workflow event
  val timerDispatchedEvent = TimerDispatchedEvent(
      timerDispatched = timerDispatched,
      workflowName = current.workflowName,
      workflowId = current.workflowId,
      workflowVersion = current.workflowVersion,
      workflowMethodName = current.workflowMethodName,
      workflowMethodId = current.workflowMethodId,
      emitterName = emitterName,
  )
  // Dispatching the workflow event
  with(producer) { timerDispatchedEvent.sendTo(WorkflowStateEventTopic) }
}
