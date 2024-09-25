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
package io.infinitic.common.transport.logged

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class LoggedInfiniticConsumer(
  private val logger: KLogger,
  private val consumer: InfiniticConsumer,
) : InfiniticConsumer {

  context(CoroutineScope)
  override suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    concurrency: Int
  ): Job {
    val loggedHandler: suspend (S, MillisInstant) -> Unit = { message, instant ->
      logger.debug { formatLog(message.id(), "Processing:", message) }
      handler(message, instant)
      logger.trace { formatLog(message.id(), "Processed:", message) }
    }

    val loggedBeforeDlq: suspend (S?, Exception) -> Unit = { message, e ->
      logger.error(e) { "Sending message to DLQ: ${message ?: "(Not Deserialized)"}." }
      beforeDlq?.let {
        logger.debug { "BeforeDlq processing..." }
        it(message, e)
        logger.trace { "BeforeDlq processed." }
      }
    }

    return consumer.startAsync(
        subscription,
        entity,
        loggedHandler,
        loggedBeforeDlq,
        concurrency,
    )
  }
}
