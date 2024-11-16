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
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.SubscriptionType
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.consumers.Result
import io.infinitic.common.transport.consumers.startBatchReceiving
import io.infinitic.common.transport.create
import io.infinitic.common.transport.interfaces.InfiniticConsumerFactory
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

context(CoroutineScope, KLogger)
internal fun InfiniticConsumerFactory.listenToServiceExecutorTopics(
  serviceName: ServiceName,
  batchConfig: BatchConfig,
  subscriptionName: String?,
  outChannel: Channel<Result<List<TransportMessage<Message>>, List<TransportMessage<Message>>>>,
): Job = launch {

  // Send messages from ServiceExecutorTopic to outChannel
  val serviceExecutorSubscription = SubscriptionType.EVENT_LISTENER.create(
      ServiceExecutorTopic,
      subscriptionName,
  )
  val serviceExecutorConsumer =
      newConsumer(serviceExecutorSubscription, serviceName.toString(), batchConfig)

  startBatchReceiving(serviceExecutorConsumer, outChannel)

  // Send messages from ServiceExecutorEventTopic to outChannel
  val serviceExecutorEventSubscription = SubscriptionType.EVENT_LISTENER.create(
      ServiceExecutorEventTopic,
      subscriptionName,
  )
  val serviceExecutorEventConsumer =
      newConsumer(serviceExecutorEventSubscription, serviceName.toString(), batchConfig)

  startBatchReceiving(serviceExecutorEventConsumer, outChannel)

  // Send messages from ServiceExecutorRetryTopic to outChannel
  val serviceExecutorRetrySubscription = SubscriptionType.EVENT_LISTENER.create(
      ServiceExecutorRetryTopic,
      subscriptionName,
  )
  val serviceExecutorRetryConsumer =
      newConsumer(serviceExecutorRetrySubscription, serviceName.toString(), batchConfig)

  startBatchReceiving(serviceExecutorRetryConsumer, outChannel)
}
