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

package io.infinitic.events.messages

import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.cloudevents.CloudEvent
import io.cloudevents.jackson.JsonFormat
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowStateCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEventMessage
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workers.InfiniticWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.delay
import kotlin.reflect.full.isSubclassOf

private val allEvents = mutableListOf<CloudEvent>()
private val events = slot<List<CloudEvent>>()
private val eventListener = mockk<CloudEventListener> {
  every { onEvents(capture(events)) } answers { allEvents.addAll(events.captured) }
}

private val transport = InMemoryTransportConfig()

private val worker = InfiniticWorker.builder()
    .setTransport(transport)
    .setStorage(InMemoryStorageConfig.builder())
    .setEventListener(
        EventListenerConfig.builder()
            .setListener(eventListener)
            .setServiceListRefreshSeconds(0.001)
            .setWorkflowListRefreshSeconds(0.001)
            .setConcurrency(2),
    )
    .build()

private val producer = transport.producerFactory.newProducer(null)

private suspend fun <T : Message> T.sendToTopic(topic: Topic<T>) {
  with(producer) { sendTo(topic) }
}

private suspend fun display() {
  delay(1200)
  allEvents.forEach { event ->
    val json = String(JsonFormat().serialize(event))
    println(jsonMapper().readTree(json).toPrettyString())
  }
  allEvents.clear()
}

suspend fun main() {
  worker.startAsync()

  ServiceExecutorMessage::class.sealedSubclasses.forEach {
    val message = TestFactory.random(it, mapOf("serviceName" to ServiceName("ServiceA")))
    message.sendToTopic(ServiceExecutorTopic)
  }

  ServiceExecutorEventMessage::class.sealedSubclasses.forEach {
    val message = TestFactory.random(it, mapOf("serviceName" to ServiceName("ServiceA")))
    message.sendToTopic(ServiceExecutorEventTopic)
  }

  WorkflowStateCmdMessage::class.sealedSubclasses.forEach {
    val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
    message.sendToTopic(WorkflowStateCmdTopic)
  }

  WorkflowStateEngineMessage::class.sealedSubclasses.forEach {
    if (!it.isSubclassOf(WorkflowStateCmdMessage::class)) {
      val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
      message.sendToTopic(WorkflowStateEngineTopic)
    }
  }

  WorkflowStateEventMessage::class.sealedSubclasses.forEach {
    val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
    message.sendToTopic(WorkflowStateEventTopic)
  }

  // wait a bit to let listener do its work
  // and the listener to discover new services and workflows
  delay(1200)

  display()

  worker.close()
}


