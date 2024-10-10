package io.infinitic.events.listeners

import io.cloudevents.CloudEvent
import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticResources
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.SubscriptionType
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import io.infinitic.common.transport.consumers.Many
import io.infinitic.common.transport.consumers.Result
import io.infinitic.common.transport.consumers.startBatching
import io.infinitic.common.transport.create
import io.infinitic.common.transport.logged.LoggedInfiniticConsumer
import io.infinitic.events.EventListener
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
  cloudEventSourcePrefix: String
): Job = launch {

  val scope = this@CoroutineScope
  val logger = this@KLogger

  val logMessageSentToDLQ: (Message?, Exception) -> Unit = { message: Message?, e: Exception ->
    error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
  }

  val channel = Channel<CloudEvent>()
  val outputChannel = Channel<Many<CloudEvent>>()

  val sendForBatching: suspend (Message, MillisInstant) -> Unit =
      { message: Message, publishedAt: MillisInstant ->
        message.toServiceCloudEvent(publishedAt, cloudEventSourcePrefix)
            ?.let { channel.send(it) }
      }

  // batching all incoming events
  channel.startBatching(
      maxMessages = config.batchConfig.maxEvents,
      maxDuration = config.batchConfig.maxMillis,
      outputChannel = outputChannel,
  )

  val inChannel: Channel<Result<TransportMessage<Message>, TransportMessage<Message>>> = Channel()

  resources.checkNewServices(config) { serviceName ->

    info { "EventListener starts listening Service $serviceName" }

    val loggedConsumer = LoggedInfiniticConsumer(EventListener.logger, this@startEventListener)

    val serviceExecutorConsumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>> =
        loggedConsumer.buildConsumer(
            SubscriptionType.EVENT_LISTENER.create(ServiceExecutorTopic, config.subscriptionName),
            serviceName.toString(),
        )
//    serviceExecutorConsumer.startConsuming(
//        inChannel,
//    )

    startServiceEventListener(serviceName, config, sendForBatching, logMessageSentToDLQ)
  }

  resources.checkNewWorkflows(config) { workflowName ->
    info { "EventListener starts listening Workflow $workflowName" }

    startWorkflowExecutorEventListener(workflowName, config, sendForBatching, logMessageSentToDLQ)

    startWorkflowStateEventListener(workflowName, config, sendForBatching, logMessageSentToDLQ)
  }
}
