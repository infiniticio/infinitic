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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticResources
import io.infinitic.common.transport.TransportMessage
import io.infinitic.common.transport.consumers.Result
import io.infinitic.common.transport.consumers.completeProcess
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.toServiceCloudEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

context(CoroutineScope, KLogger)
fun InfiniticConsumer.startEventListener(
  resources: InfiniticResources,
  config: EventListenerConfig,
  cloudEventSourcePrefix: String,
): Job = launch {

  // Channels where all messages consumed from topics are sent
  val outChannel = Channel<Result<TransportMessage<Message>, TransportMessage<Message>>>()

  // all messages will have this batch config
  val batchConfig = BatchConfig(
      batchKey = "cloudEvent", // same for all
      maxMessages = config.batchConfig.maxEvents,
      maxDuration = MillisDuration(config.batchConfig.maxMillis),
  )

  // Launch the complete processing of outChannel
  launch {
    outChannel
        .completeProcess(
            concurrency = config.concurrency,
            deserialize = { it.deserialize() },
            process = { message, publishTime -> thisShouldNotHappen() },
            batchConfig = { batchConfig },
            batchProcess = { messages, publishingTimes ->
              val cloudEvents = messages.zip(publishingTimes) { message, publishTime ->
                message.toServiceCloudEvent(publishTime, cloudEventSourcePrefix)
              }.filterNotNull()
              if (cloudEvents.isNotEmpty()) {
                config.listener.onEvents(cloudEvents)
              }
            },
            maxRedeliver = 3, // TODO
        )
  }

  // Listen service topics, for each service found
  resources.checkNewServices(config) { serviceName ->
    info { "EventListener starts listening Service $serviceName" }

    listenToServiceExecutorTopics(serviceName, config.subscriptionName, outChannel)
  }

  // Listen workflow topics, for each workflow found
  resources.checkNewWorkflows(config) { workflowName ->
    info { "EventListener starts listening Workflow $workflowName" }

    listenToWorkflowExecutorTopics(workflowName, config.subscriptionName, outChannel)

    listenToWorkflowStateTopics(workflowName, config.subscriptionName, outChannel)
  }
}
