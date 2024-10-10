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
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodCanceled
import io.infinitic.common.workflows.engine.messages.RemoteMethodCompleted
import io.infinitic.common.workflows.engine.messages.RemoteMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.RemoteMethodFailed
import io.infinitic.common.workflows.engine.messages.RemoteMethodTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteMethodUnknown
import io.infinitic.common.workflows.engine.messages.RemoteTaskCanceled
import io.infinitic.common.workflows.engine.messages.RemoteTaskCompleted
import io.infinitic.common.workflows.engine.messages.RemoteTaskFailed
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.RemoteTimerCompleted
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.SignalDiscardedEvent
import io.infinitic.common.workflows.engine.messages.SignalDispatchedEvent
import io.infinitic.common.workflows.engine.messages.SignalReceivedEvent
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.TimerDispatchedEvent
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.workers.InfiniticWorker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.bytebuddy.utility.RandomString
import java.net.URI
import kotlin.reflect.full.isSubclassOf

private val events = mutableListOf<CloudEvent>()
private val eventListener = mockk<CloudEventListener> {
  every { onEvents(capture(events)) } just Runs
}

private val transport = InMemoryTransportConfig()

private val worker = InfiniticWorker.builder()
    .setTransport(transport)
    .setStorage(InMemoryStorageConfig.builder())
    .setEventListener(
        EventListenerConfig.builder()
            .setListener(eventListener)
            .setServiceListRefreshSeconds(0.0)
            .setWorkflowListRefreshSeconds(0.0)
            .setConcurrency(2),
    )
    .build()

private suspend fun <T : Message> T.sendToTopic(topic: Topic<T>) {
  with(transport.producer) { sendTo(topic) }
// wait a bit to let listener do its work
// and the listener to discover new services and workflows
  delay(100)
}

suspend fun main() {
  worker.startAsync()

  ServiceExecutorMessage::class.sealedSubclasses.forEach {
    events.clear()
    val message = TestFactory.random(it, mapOf("serviceName" to ServiceName("ServiceA")))
    message.sendToTopic(ServiceExecutorTopic)

    events.firstOrNull()?.let { event ->
      val json = String(JsonFormat().serialize(event))
      println(message)
      println(jsonMapper().readTree(json).toPrettyString())
    }
  }

  ServiceExecutorEventMessage::class.sealedSubclasses.forEach {
    events.clear()
    val message = TestFactory.random(it, mapOf("serviceName" to ServiceName("ServiceA")))
    message.sendToTopic(ServiceExecutorEventTopic)
    events.firstOrNull()?.let { event ->
      val json = String(JsonFormat().serialize(event))
      println(message)
      println(jsonMapper().readTree(json).toPrettyString())
    }
  }

  WorkflowStateEngineCmdMessage::class.sealedSubclasses.forEach {
    events.clear()
    val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
    message.sendToTopic(WorkflowStateCmdTopic)
    events.firstOrNull()?.let { event ->
      val json = String(JsonFormat().serialize(event))
      println(message)
      println(jsonMapper().readTree(json).toPrettyString())
    }
  }

  WorkflowStateEngineMessage::class.sealedSubclasses.forEach {
    if (!it.isSubclassOf(WorkflowStateEngineCmdMessage::class)) {
      events.clear()
      val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
      message.sendToTopic(WorkflowStateEngineTopic)
      events.firstOrNull()?.let { event ->
        val json = String(JsonFormat().serialize(event))
        println(message)
        println(jsonMapper().readTree(json).toPrettyString())
      }
    }
  }

  WorkflowStateEngineEventMessage::class.sealedSubclasses.forEach {
    events.clear()
    val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
    message.sendToTopic(WorkflowStateEventTopic)
    events.firstOrNull()?.let { event ->
      val json = String(JsonFormat().serialize(event))
      println(message)
      println(jsonMapper().readTree(json).toPrettyString())
    }
  }

  worker.close()
}

internal class CloudEventTests : StringSpec(
    {
      beforeSpec {
        worker.startAsync()
      }

      afterSpec {
        worker.close()
      }

      beforeEach {
        events.clear()
      }

      ServiceExecutorMessage::class.sealedSubclasses.forEach {
        "Check ${it.simpleName} event envelope from Service Executor topic" {
          val message = TestFactory.random(it, mapOf("serviceName" to ServiceName("ServiceA")))
          message.sendToTopic(ServiceExecutorTopic)

// first test is slow down in GitHub
          delay(2000)

          events.size shouldBe 1
          val event = events.first()
          event.id shouldBe message.messageId.toString()
          event.source shouldBe URI("inMemory/services/executor/ServiceA")
          event.dataContentType shouldBe "application/json"
          event.subject shouldBe message.taskId.toString()
          event.type shouldBe when (it) {
            ExecuteTask::class -> "infinitic.task.start"
            else -> thisShouldNotHappen()
          }
        }
      }

      ServiceExecutorEventMessage::class.sealedSubclasses.forEach {
        "Check ${it.simpleName} event envelope from Service Events topic" {
          val message = TestFactory.random(it, mapOf("serviceName" to ServiceName("ServiceA")))
          message.sendToTopic(ServiceExecutorEventTopic)

          events.size shouldBe 1
          val event = events.first()
          event.id shouldBe message.messageId.toString()
          event.source shouldBe URI("inMemory/services/executor/ServiceA")
          event.dataContentType shouldBe "application/json"
          event.subject shouldBe message.taskId.toString()
          event.type shouldBe when (it) {
            TaskStartedEvent::class -> "infinitic.task.started"
            TaskCompletedEvent::class -> when ((message as TaskCompletedEvent).isDelegated) {
              true -> "infinitic.task.delegationCompleted"
              false -> "infinitic.task.completed"
            }

            TaskFailedEvent::class -> "infinitic.task.failed"
            TaskRetriedEvent::class -> "infinitic.task.retryScheduled"
            else -> thisShouldNotHappen()
          }
        }
      }

      ServiceExecutorMessage::class.sealedSubclasses.forEach {
        "Check ${it.simpleName} source for WorkflowTask" {
          val message = TestFactory.random(
              it,
              mapOf(
                  "serviceName" to WorkflowTask.WORKFLOW_SERVICE_NAME,
                  "workflowName" to WorkflowName("WorkflowA"),
              ),
          )
          message.sendToTopic(WorkflowExecutorTopic)

          events.size shouldBe 1
          val event = events.first()
          event.source shouldBe URI("inMemory/workflows/executor/WorkflowA")
          event.subject shouldBe message.taskId.toString()
          event.type shouldBe when (it) {
            ExecuteTask::class -> "infinitic.task.start"
            else -> thisShouldNotHappen()
          }
        }
      }

      ServiceExecutorEventMessage::class.sealedSubclasses.forEach {
        "Check ${it.simpleName} source for WorkflowTask" {
          var message = TestFactory.random(
              it,
              mapOf(
                  "serviceName" to WorkflowTask.WORKFLOW_SERVICE_NAME,
                  "workflowName" to WorkflowName("WorkflowA"),
              ),
          )
          if (message is TaskCompletedEvent) {
            message = (message as TaskCompletedEvent).copy(isDelegated = false)
          }
          message.sendToTopic(WorkflowExecutorEventTopic)

          events.size shouldBe 1
          val event = events.first()
          event.source shouldBe URI("inMemory/workflows/executor/WorkflowA")
          event.subject shouldBe message.taskId.toString()
          event.type shouldBe when (it) {
            TaskStartedEvent::class -> "infinitic.task.started"
            TaskCompletedEvent::class -> "infinitic.task.completed"
            TaskFailedEvent::class -> "infinitic.task.failed"
            TaskRetriedEvent::class -> "infinitic.task.retryScheduled"
            else -> thisShouldNotHappen()
          }
        }
      }

      WorkflowStateEngineCmdMessage::class.sealedSubclasses.forEach {
        "Check ${it.simpleName} event envelope from cmd topic" {
          val message = TestFactory.random(it, mapOf("workflowName" to WorkflowName("WorkflowA")))
          message.sendToTopic(WorkflowStateCmdTopic)

          val type = when (it) {
            CancelWorkflow::class -> when ((message as CancelWorkflow).workflowMethodId) {
              null -> "infinitic.workflow.cancel"
              else -> "infinitic.workflow.cancelMethod"
            }

            CompleteTimers::class -> null
            CompleteWorkflow::class -> null
            DispatchMethod::class -> "infinitic.workflow.startMethod"
            DispatchWorkflow::class -> "infinitic.workflow.start"
            RetryTasks::class -> "infinitic.workflow.retryTask"
            RetryWorkflowTask::class -> "infinitic.workflow.retryExecutor"
            SendSignal::class -> "infinitic.workflow.signal"
            WaitWorkflow::class -> null
            else -> thisShouldNotHappen()
          }

          events.size shouldBe if (type == null) 0 else 1
          if (events.size == 1) {
            val event = events.first()
            event.id shouldBe message.messageId.toString()
            event.source shouldBe URI("inMemory/workflows/stateEngine/WorkflowA")
            event.dataContentType shouldBe "application/json"
            event.subject shouldBe message.workflowId.toString()
            event.type shouldBe type
          }
        }
      }

// TODO complete this test and add similar tests for all other events
      "Check infinitic.task.dispatched data" {
        val message = TestFactory.random<ExecuteTask>(
            mapOf("serviceName" to ServiceName("ServiceA")),
        )
        message.sendToTopic(ServiceExecutorTopic)

        events.size shouldBe 1
        val event = events.first()
        val json = Json.parseToJsonElement(String(event.data!!.toBytes())).jsonObject
        json["taskName"]!!.jsonPrimitive.content shouldBe message.methodName.toString()
      }

      WorkflowStateEngineCmdMessage::class.sealedSubclasses.forEach {
        "No ${it.simpleName} event should come from engine topic" {
          val message = TestFactory.random(
              it,
              mapOf(
                  "workflowName" to WorkflowName("WorkflowA"),
                  "requester" to ClientRequester(clientName = ClientName(RandomString().nextString())),
              ),
          )
          message.sendToTopic(WorkflowStateEngineTopic)
          events.size shouldBe 0
        }
      }

      WorkflowStateEngineMessage::class.sealedSubclasses.forEach {
        if (!it.isSubclassOf(WorkflowStateEngineCmdMessage::class)) {
          "Check ${it.simpleName} event envelope from engine topic" {
            val message = TestFactory.random(
                it,
                mapOf(
                    "workflowName" to WorkflowName("WorkflowA"),
                ),
            )
            message.sendToTopic(WorkflowStateEngineTopic)

            val type = when (it) {
              RemoteMethodCanceled::class -> "infinitic.workflow.remoteMethodCanceled"
              RemoteMethodCompleted::class -> "infinitic.workflow.remoteMethodCompleted"
              RemoteMethodFailed::class -> "infinitic.workflow.remoteMethodFailed"
              RemoteMethodTimedOut::class -> "infinitic.workflow.remoteMethodTimedOut"
              RemoteMethodUnknown::class -> "infinitic.workflow.remoteMethodUnknown"
              RemoteTaskCanceled::class -> null
              RemoteTaskCompleted::class -> "infinitic.workflow.taskCompleted"
              RemoteTaskFailed::class -> "infinitic.workflow.taskFailed"
              RemoteTaskTimedOut::class -> "infinitic.workflow.taskTimedOut"
              RemoteTimerCompleted::class -> "infinitic.workflow.timerCompleted"
              else -> thisShouldNotHappen()
            }

            events.size shouldBe if (type == null) 0 else 1
            if (events.size == 1) {
              val event = events.first()
              event.id shouldBe message.messageId.toString()
              event.source shouldBe URI("inMemory/workflows/stateEngine/WorkflowA")
              event.dataContentType shouldBe "application/json"
              event.subject shouldBe message.workflowId.toString()
              event.type shouldBe type
            }
          }
        }
      }

      WorkflowStateEngineEventMessage::class.sealedSubclasses.forEach {
        "Check ${it.simpleName} event envelope from events topic" {
          val message = TestFactory.random(
              it,
              mapOf("workflowName" to WorkflowName("WorkflowA")),
          )
          message.sendToTopic(WorkflowStateEventTopic)

          val type = when (it) {
            WorkflowCompletedEvent::class -> "infinitic.workflow.ended"
            WorkflowCanceledEvent::class -> "infinitic.workflow.canceled"
            MethodCommandedEvent::class -> "infinitic.workflow.startMethod"
            MethodCompletedEvent::class -> "infinitic.workflow.methodCompleted"
            MethodFailedEvent::class -> "infinitic.workflow.methodFailed"
            MethodCanceledEvent::class -> "infinitic.workflow.methodCanceled"
            MethodTimedOutEvent::class -> "infinitic.workflow.methodTimedOut"
            TaskDispatchedEvent::class -> "infinitic.workflow.taskDispatched"
            RemoteMethodDispatchedEvent::class -> "infinitic.workflow.remoteMethodDispatched"
            TimerDispatchedEvent::class -> "infinitic.workflow.timerDispatched"
            SignalDispatchedEvent::class -> "infinitic.workflow.signalDispatched"
            SignalReceivedEvent::class -> "infinitic.workflow.signalReceived"
            SignalDiscardedEvent::class -> "infinitic.workflow.signalDiscarded"
            else -> thisShouldNotHappen()
          }

          events.size shouldBe 1
          if (events.size == 1) {
            val event = events.first()
            event.id shouldBe message.messageId.toString()
            event.source shouldBe URI("inMemory/workflows/stateEngine/WorkflowA")
            event.dataContentType shouldBe "application/json"
            event.subject shouldBe message.workflowId.toString()
            event.type shouldBe type
          }
        }
      }
    },
) {

}

