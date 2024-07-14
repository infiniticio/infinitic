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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.workflows.data.commands.CommandStatus.Completed
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.SignalDiscardedEvent
import io.infinitic.common.workflows.engine.messages.SignalReceivedEvent
import io.infinitic.common.workflows.engine.messages.data.SignalDiscarded
import io.infinitic.common.workflows.engine.messages.data.SignalReceived
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.helpers.commandTerminated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

internal fun CoroutineScope.sendSignal(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: SendSignal
) {
  val emitterName = EmitterName(producer.name)

  state.receivingChannels
      .firstOrNull {
        it.channelName == message.channelName &&
            (it.channelType == null || message.channelTypes.contains(it.channelType)) &&
            (it.channelFilter == null || it.channelFilter!!.check(message.signalData))
      }
      ?.also {
        it.receivedSignalCount += 1

        // remove this ReceivingChannel from state.receivingChannels if this signal is the last one
        if (it.receivedSignalCount == it.receivedSignalLimit) {
          state.receivingChannels.remove(it)
        }

        commandTerminated(
            producer,
            state,
            it.workflowMethodId,
            it.commandId,
            Completed(
                returnIndex = it.receivedSignalCount - 1,
                returnValue = MethodReturnValue(message.signalData.serializedData),
                completionWorkflowTaskIndex = state.workflowTaskIndex,
                signalId = message.signalId,
            ),
            message.emittedAt ?: thisShouldNotHappen(),
        )

        // Event: signal received
        launch {
          val signalReceivedEvent = SignalReceivedEvent(
              signalReceived = SignalReceived(signalId = message.signalId),
              workflowName = message.workflowName,
              workflowId = message.workflowId,
              workflowVersion = state.workflowVersion,
              emitterName = emitterName,
          )
          with(producer) { signalReceivedEvent.sendTo(WorkflowEventsTopic) }
        }

      } ?: launch {
    logger.debug { "discarding non-waited signal $message" }

    // Event: signal discarded
    val signalDiscardedEvent = SignalDiscardedEvent(
        signalDiscarded = SignalDiscarded(signalId = message.signalId),
        workflowName = message.workflowName,
        workflowId = message.workflowId,
        workflowVersion = state.workflowVersion,
        emitterName = emitterName,
    )

    with(producer) { signalDiscardedEvent.sendTo(WorkflowEventsTopic) }
  }
}
