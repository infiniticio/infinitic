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
package io.infinitic.workflows.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workflows.engine.events.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.events.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.events.WorkflowMethodCanceledEvent
import io.infinitic.common.workflows.engine.events.WorkflowMethodCompletedEvent
import io.infinitic.common.workflows.engine.events.WorkflowMethodFailedEvent
import io.infinitic.common.workflows.engine.events.WorkflowMethodStartedEvent
import io.infinitic.common.workflows.engine.events.WorkflowMethodTimedOutEvent
import io.infinitic.common.workflows.engine.events.WorkflowStartedEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class WorkflowEventHandler(producerAsync: InfiniticProducerAsync) {

  private val logger = KotlinLogging.logger(javaClass.name)
  val producer = LoggedInfiniticProducer(javaClass.name, producerAsync)

  @Suppress("UNUSED_PARAMETER")
  suspend fun handle(msg: WorkflowEventMessage, publishTime: MillisInstant) {
    msg.logDebug { "received $msg" }

    when (msg) {
      is WorkflowStartedEvent -> Unit
      is WorkflowCanceledEvent -> Unit
      is WorkflowCompletedEvent -> Unit
      is WorkflowMethodStartedEvent -> Unit
      is WorkflowMethodCanceledEvent -> sendWorkflowMethodCanceled(msg)
      is WorkflowMethodCompletedEvent -> sendWorkflowMethodCompleted(msg)
      is WorkflowMethodFailedEvent -> sendWorkflowMethodFailed(msg)
      is WorkflowMethodTimedOutEvent -> sendWorkflowMethodTimedOut(msg)
    }

    msg.logTrace { "processed" }
  }

  private suspend fun sendWorkflowMethodCanceled(msg: WorkflowMethodCanceledEvent) =
      coroutineScope {
        // tell waiting clients
        msg.getEventsForClient().forEach {
          launch { producer.sendToClient(it) }
        }

        // tell parent workflow (except itself) if any
        val event = msg.getEventForParentWorkflow()

        if (event != null && !msg.isItsOwnParent()) launch {
          producer.sendToWorkflowEngine(event)
        }
      }

  private suspend fun sendWorkflowMethodCompleted(msg: WorkflowMethodCompletedEvent) =
      coroutineScope {
        // tell waiting clients
        msg.getEventsForClient().forEach {
          launch { producer.sendToClient(it) }
        }

        // tell parent workflow (except itself) if any
        val event = msg.getEventForParentWorkflow()

        if (event != null && !msg.isItsOwnParent()) launch {
          producer.sendToWorkflowEngine(event)
        }
      }

  private suspend fun sendWorkflowMethodFailed(msg: WorkflowMethodFailedEvent) =
      coroutineScope {
        // tell waiting clients
        msg.getEventsForClient().forEach {
          launch { producer.sendToClient(it) }
        }

        // tell parent workflow (except itself) if any
        val event = msg.getEventForParentWorkflow()

        if (event != null && !msg.isItsOwnParent()) launch {
          producer.sendToWorkflowEngine(event)
        }
      }

  private suspend fun sendWorkflowMethodTimedOut(msg: WorkflowMethodTimedOutEvent) =
      coroutineScope {
        // tell waiting clients
        msg.getEventsForClient().forEach {
          launch { producer.sendToClient(it) }
        }

        // tell parent workflow (except itself) if any
        val event = msg.getEventForParentWorkflow()

        if (event != null && !msg.isItsOwnParent()) launch {
          producer.sendToWorkflowEngine(event)
        }
      }

  private fun WorkflowEventMessage.logDebug(description: () -> String) {
    logger.debug { "$workflowName (${workflowId}): ${description()}" }
  }

  private fun WorkflowEventMessage.logTrace(description: () -> String) {
    logger.trace { "$workflowName (${workflowId}): ${description()}" }
  }
}
