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

import io.cloudevents.CloudEvent
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.events.CloudEventListener
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.ChildMethodDispatchedEvent
import io.infinitic.common.workflows.engine.messages.ChildMethodFailed
import io.infinitic.common.workflows.engine.messages.ChildMethodTimedOut
import io.infinitic.common.workflows.engine.messages.ChildMethodUnknown
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.CompleteWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodCanceledEvent
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.MethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.MethodFailedEvent
import io.infinitic.common.workflows.engine.messages.MethodTimedOutEvent
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.TaskCanceled
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCanceledEvent
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowCompletedEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.WorkerConfig
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

private val serviceConfig = """
transport: inMemory
storage: inMemory
"""
private val workerConfig = WorkerConfig.fromYaml(serviceConfig)
private val events = mutableListOf<CloudEvent>()
private val listener = mockk<CloudEventListener>() {
  every { onEvent(capture(events)) } just Runs
}
private val worker = InfiniticWorker.fromConfig(workerConfig).apply {
  registerServiceEventListener("ServiceA", 2, listener, null)
  registerWorkflowEventListener("WorkflowA", 2, listener, null)
  startAsync()
}

private suspend fun <T : Message> T.sendToTopic(topic: Topic<T>) {
  with(worker.producerAsync) { sendToAsync(topic).join() }
  // wait a bit to let listener do its work
  delay(200)
}

internal class CloudEventTests :
  StringSpec(
      {
        beforeTest {
          events.clear()
        }

        afterSpec {
          worker.close()
        }

        ServiceExecutorMessage::class.sealedSubclasses.forEach {
          "Check ${it.simpleName} event envelope from Service Executor topic" {
            val message = TestFactory.random(
                it,
                mapOf("serviceName" to ServiceName("ServiceA")),
            )
            message.sendToTopic(ServiceExecutorTopic)

            events.size shouldBe 1
            val event = events.first()
            event.id shouldBe message.messageId.toString()
            event.source shouldBe URI("inmemory/services/ServiceA")
            event.dataContentType shouldBe "application/json"
            event.subject shouldBe message.taskId.toString()
            event.type shouldBe when (it) {
              ExecuteTask::class -> "infinitic.task.commanded"
              else -> thisShouldNotHappen()
            }
          }
        }

        ServiceEventMessage::class.sealedSubclasses.forEach {
          "Check ${it.simpleName} event envelope from Service Events topic" {
            val message = TestFactory.random(
                it,
                mapOf("serviceName" to ServiceName("ServiceA")),
            )
            message.sendToTopic(ServiceEventsTopic)

            events.size shouldBe 1
            val event = events.first()
            event.id shouldBe message.messageId.toString()
            event.source shouldBe URI("inmemory/services/ServiceA")
            event.dataContentType shouldBe "application/json"
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

        ServiceExecutorMessage::class.sealedSubclasses.forEach {
          "Check ${it.simpleName} source for WorkflowTask" {
            val message = TestFactory.random(
                it,
                mapOf(
                    "serviceName" to ServiceName(WorkflowTask::class.java.name),
                    "workflowName" to WorkflowName("WorkflowA"),
                ),
            )
            message.sendToTopic(WorkflowTaskExecutorTopic)

            events.size shouldBe 1
            val event = events.first()
            event.source shouldBe URI("inmemory/services/executor/WorkflowA")
            event.subject shouldBe message.taskId.toString()
            event.type shouldBe when (it) {
              ExecuteTask::class -> "infinitic.task.commanded"
              else -> thisShouldNotHappen()
            }
          }
        }

        ServiceEventMessage::class.sealedSubclasses.forEach {
          "Check ${it.simpleName} source for WorkflowTask" {
            val message = TestFactory.random(
                it,
                mapOf(
                    "serviceName" to ServiceName(WorkflowTask::class.java.name),
                    "workflowName" to WorkflowName("WorkflowA"),
                ),
            )
            message.sendToTopic(WorkflowTaskEventsTopic)

            events.size shouldBe 1
            val event = events.first()
            event.source shouldBe URI("inmemory/services/executor/WorkflowA")
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

        WorkflowCmdMessage::class.sealedSubclasses.forEach {
          "Check ${it.simpleName} event envelope from cmd topic" {
            val message = TestFactory.random(
                it,
                mapOf("workflowName" to WorkflowName("WorkflowA")),
            )
            message.sendToTopic(WorkflowCmdTopic)

            val type = when (it) {
              CancelWorkflow::class -> when ((message as CancelWorkflow).workflowMethodId) {
                null -> "infinitic.workflow.cancelCommanded"
                else -> "infinitic.workflow.method.cancelCommanded"
              }

              CompleteTimers::class -> null
              CompleteWorkflow::class -> null
              DispatchMethod::class -> "infinitic.workflow.method.commanded"
              DispatchWorkflow::class -> "infinitic.workflow.commanded"
              RetryTasks::class -> "infinitic.workflow.remote.task.retryCommanded"
              RetryWorkflowTask::class -> "infinitic.workflow.executor.retryCommanded"
              SendSignal::class -> "infinitic.workflow.signal.commanded"
              WaitWorkflow::class -> null
              else -> thisShouldNotHappen()
            }

            events.size shouldBe if (type == null) 0 else 1
            if (events.size == 1) {
              val event = events.first()
              event.id shouldBe message.messageId.toString()
              event.source shouldBe URI("inmemory/workflows/WorkflowA")
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

        WorkflowCmdMessage::class.sealedSubclasses.forEach {
          "No ${it.simpleName} event should come from engine topic" {
            val message = TestFactory.random(
                it,
                mapOf(
                    "workflowName" to WorkflowName("WorkflowA"),
                    "requester" to ClientRequester(clientName = ClientName(RandomString().nextString())),
                ),
            )
            message.sendToTopic(WorkflowEngineTopic)
            events.size shouldBe 0
          }
        }

        WorkflowEngineMessage::class.sealedSubclasses.forEach {
          if (!it.isSubclassOf(WorkflowCmdMessage::class)) {
            "Check ${it.simpleName} event envelope from engine topic" {
              val message = TestFactory.random(
                  it,
                  mapOf("workflowName" to WorkflowName("WorkflowA")),
              )
              message.sendToTopic(WorkflowEngineTopic)

              val type = when (it) {
                ChildMethodCanceled::class -> "infinitic.workflow.remote.workflow.canceled"
                ChildMethodCompleted::class -> "infinitic.workflow.remote.workflow.completed"
                ChildMethodFailed::class -> "infinitic.workflow.remote.workflow.failed"
                ChildMethodTimedOut::class -> "infinitic.workflow.remote.workflow.timedOut"
                ChildMethodUnknown::class -> "infinitic.workflow.remote.workflow.unknown"
                TaskCanceled::class -> null
                TaskCompleted::class -> "infinitic.workflow.remote.task.completed"
                TaskFailed::class -> "infinitic.workflow.remote.task.failed"
                TaskTimedOut::class -> "infinitic.workflow.remote.task.timedOut"
                TimerCompleted::class -> "infinitic.workflow.remote.timer.completed"
                else -> thisShouldNotHappen()
              }

              events.size shouldBe if (type == null) 0 else 1
              if (events.size == 1) {
                val event = events.first()
                event.id shouldBe message.messageId.toString()
                event.source shouldBe URI("inmemory/workflows/WorkflowA")
                event.dataContentType shouldBe "application/json"
                event.subject shouldBe message.workflowId.toString()
                event.type shouldBe type
              }
            }
          }
        }

        WorkflowEventMessage::class.sealedSubclasses.forEach {
          "Check ${it.simpleName} event envelope from engine topic" {
            val message = TestFactory.random(
                it,
                mapOf("workflowName" to WorkflowName("WorkflowA")),
            )
            message.sendToTopic(WorkflowEventsTopic)

            val type = when (it) {
              WorkflowCompletedEvent::class -> "infinitic.workflow.completed"
              WorkflowCanceledEvent::class -> "infinitic.workflow.canceled"
              MethodCommandedEvent::class -> "infinitic.workflow.method.commanded"
              MethodCompletedEvent::class -> "infinitic.workflow.method.completed"
              MethodFailedEvent::class -> "infinitic.workflow.method.failed"
              MethodCanceledEvent::class -> "infinitic.workflow.method.canceled"
              MethodTimedOutEvent::class -> "infinitic.workflow.method.timedOut"
              TaskDispatchedEvent::class -> "infinitic.workflow.remote.task.dispatched"
              ChildMethodDispatchedEvent::class -> "infinitic.workflow.remote.workflow.dispatched"
              else -> thisShouldNotHappen()
            }

            events.size shouldBe 1
            if (events.size == 1) {
              val event = events.first()
              event.id shouldBe message.messageId.toString()
              event.source shouldBe URI("inmemory/workflows/WorkflowA")
              event.dataContentType shouldBe "application/json"
              event.subject shouldBe message.workflowId.toString()
              event.type shouldBe type
            }
          }
        }
      },
  ) {

}
