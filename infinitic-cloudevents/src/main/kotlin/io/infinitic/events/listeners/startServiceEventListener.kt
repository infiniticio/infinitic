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
package io.infinitic.events.listeners

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.SubscriptionType
import io.infinitic.common.transport.create
import io.infinitic.common.transport.logged.LoggedInfiniticConsumer
import io.infinitic.events.EventListener
import io.infinitic.events.config.EventListenerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

context(CoroutineScope, KLogger)
fun InfiniticConsumer.startServiceEventListener(
  serviceName: ServiceName,
  config: EventListenerConfig,
  process: suspend (Message, MillisInstant) -> Unit,
  logMessageSentToDLQ: (Message?, Exception) -> Unit
): Job = launch {
  val logger = EventListener.logger

  val loggedConsumer = LoggedInfiniticConsumer(logger, this@startServiceEventListener)

  // TASK-EXECUTOR topic
  loggedConsumer.startAsync(
      subscription = SubscriptionType.EVENT_LISTENER.create(
          ServiceExecutorTopic,
          config.subscriptionName,
      ),
      entity = serviceName.toString(),
      concurrency = config.concurrency,
      process = process,
      beforeDlq = logMessageSentToDLQ,
  )

  // TASK-RETRY topic
  loggedConsumer.startAsync(
      subscription = SubscriptionType.EVENT_LISTENER.create(
          ServiceExecutorRetryTopic,
          config.subscriptionName,
      ),
      entity = serviceName.toString(),
      concurrency = config.concurrency,
      process = process,
      beforeDlq = logMessageSentToDLQ,
  )

  // TASK-EVENTS topic
  loggedConsumer.startAsync(
      subscription = SubscriptionType.EVENT_LISTENER.create(
          ServiceExecutorEventTopic,
          config.subscriptionName,
      ),
      entity = serviceName.toString(),
      concurrency = config.concurrency,
      process = process,
      beforeDlq = logMessageSentToDLQ,
  )
}
