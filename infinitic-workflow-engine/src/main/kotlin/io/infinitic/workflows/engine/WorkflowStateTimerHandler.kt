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
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.transport.withoutDelay
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import java.time.Instant

@Suppress("UNUSED_PARAMETER")
class WorkflowStateTimerHandler(
  val producer: InfiniticProducer,
  pastDueSeconds: Long
) {
  private val pastDueMillis = pastDueSeconds * 1000

  suspend fun process(message: WorkflowStateEngineMessage, publishTime: MillisInstant) {
    if (message is RemoteTimerCompleted) {
      // Workaround for Pulsar: see https://github.com/apache/pulsar/discussions/23990
      // When brokers restart, already acknowledged delayed messages may reappear.
      // If a RemoteTimerCompleted message was supposed to be received over 72 hours ago, discard it
      // to prevent unnecessary reprocessing. (WorkflowStateEngine would discard it anyway,
      // but this avoids extra processing earlier in the pipeline.)
      message.emittedAt?.let {
        // If the emittedAt is more than 3 days ago (default), we discard the message
        if (Instant.now().toEpochMilli() - it.long > pastDueMillis) {
          logger.warn { "RemoteTimerCompleted discarded as too old: $message" }
          return
        }
      }
    }
    // Forward the message to the WorkflowStateEngineTopic
    logger.trace { "Sending to WorkflowStateEngineTopic: $message" }
    producer.internalSendTo(message, WorkflowStateEngineTopic.withoutDelay)
    logger.debug { "Sent to WorkflowStateEngineTopic: $message" }
  }

  companion object {
    val logger = LoggerWithCounter(KotlinLogging.logger {})
  }
}
