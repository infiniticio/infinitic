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
package io.infinitic.common.workflows.engine.commands

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.data.DurationTimerDispatched
import io.infinitic.common.workflows.engine.messages.data.InstantTimerDispatched
import io.infinitic.common.workflows.engine.messages.data.TimerDispatched

suspend fun InfiniticProducer.dispatchTimer(
  timerDispatched: TimerDispatched,
  requester: WorkflowRequester
) {
  val emitterName = EmitterName(getProducerName())

  val timerInstant = when (timerDispatched) {
    is InstantTimerDispatched -> timerDispatched.timerInstant
    is DurationTimerDispatched -> timerDispatched.emittedAt + timerDispatched.duration
  }

  val remoteTimerCompleted = RemoteTimerCompleted(
      timerId = timerDispatched.timerId,
      workflowName = requester.workflowName,
      workflowId = requester.workflowId,
      workflowVersion = requester.workflowVersion,
      workflowMethodName = requester.workflowMethodName,
      workflowMethodId = requester.workflowMethodId,
      emitterName = emitterName,
      emittedAt = timerInstant,
  )

  // todo: Check if there is a way not to use MillisInstant.now()
  val delay = timerInstant - MillisInstant.now()
  remoteTimerCompleted.sendTo(io.infinitic.common.transport.WorkflowStateTimerTopic, delay)
}
