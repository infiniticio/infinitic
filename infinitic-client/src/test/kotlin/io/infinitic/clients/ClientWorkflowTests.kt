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
package io.infinitic.clients

import io.infinitic.clients.deferred.DeferredMethod
import io.infinitic.clients.samples.FakeClass
import io.infinitic.clients.samples.FakeInterface
import io.infinitic.clients.samples.FakeTask
import io.infinitic.clients.samples.FakeTaskImpl
import io.infinitic.clients.samples.FakeTaskParent
import io.infinitic.clients.samples.FakeWorkflow
import io.infinitic.clients.samples.FakeWorkflowImpl
import io.infinitic.clients.samples.FooWorkflow
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.InvalidStubException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

private val taskTagSlots = CopyOnWriteArrayList<TaskTagMessage>() // multithreading update
private val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagMessage>() // multithreading update
private val taskSlot = slot<TaskExecutorMessage>()
private val workflowEngineSlot = slot<WorkflowEngineMessage>()
private val clientNameTest = ClientName("clientTest")

fun tagResponse(): CompletableFuture<Unit> {
  workflowTagSlots.forEach {
    if (it is GetWorkflowIdsByTag) {
      val workflowIdsByTag =
          WorkflowIdsByTag(
              recipientName = ClientName(client.name),
              workflowName = it.workflowName,
              workflowTag = it.workflowTag,
              workflowIds = setOf(WorkflowId(), WorkflowId()),
              emitterName = ClientName("mockk"),
          )
      CoroutineScope(Dispatchers.IO).launch {
        delay(100)
        client.handle(workflowIdsByTag)
      }
    }
  }
  return CompletableFuture.completedFuture(null)
}

fun engineResponse(): CompletableFuture<Unit> {
  val msg = workflowEngineSlot.captured
  if (msg is DispatchWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
    val methodCompleted = MethodCompleted(
        recipientName = ClientName(client.name),
        workflowId = msg.workflowId,
        methodRunId = MethodRunId.from(msg.workflowId),
        methodReturnValue = ReturnValue.from("success"),
        emitterName = ClientName("mockk"),
    )
    CoroutineScope(Dispatchers.IO).launch {
      delay(100)
      client.handle(methodCompleted)
    }
  }
  return CompletableFuture.completedFuture(null)
}

val delaySlot = slot<MillisDuration>()
val producer: InfiniticProducer = mockk<InfiniticProducer> {
  every { name } returns "$clientNameTest"

  every { sendAsync(capture(workflowTagSlots)) } answers { tagResponse() }
  coEvery { send(capture(workflowTagSlots)) } answers { tagResponse().join() }

  every { sendAsync(capture(workflowEngineSlot), capture(delaySlot)) } answers { engineResponse() }
  coEvery {
    send(
        capture(workflowEngineSlot),
        capture(delaySlot),
    )
  } answers { engineResponse().join() }
}

val consumer: InfiniticConsumer = mockk<InfiniticConsumer> {
  every {
    startClientConsumerAsync(any(), clientNameTest)
  } returns CompletableFuture.completedFuture(null)
}

val client = InfiniticClient(consumer, producer)

class ClientWorkflowTests : StringSpec(
    {
      val meta: Map<String, ByteArray> =
          mapOf("foo" to TestFactory.random(), "bar" to TestFactory.random())
      val tags = setOf("foo", "bar")

      val fakeWorkflow = client.newWorkflow(FakeWorkflow::class.java)
      val fooWorkflow = client.newWorkflow(FooWorkflow::class.java)

      val fakeWorkflowWithMeta = client.newWorkflow(FakeWorkflow::class.java, meta = meta)
      val fakeWorkflowWithTags = client.newWorkflow(FakeWorkflow::class.java, tags = tags)

      beforeTest {
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowEngineSlot.clear()
      }

      "Should be able to dispatch a workflow" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m0)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("m0"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )

        // when asynchronously dispatching a workflow, the consumer should not be started
        verify { consumer.startClientConsumerAsync(any(), any()) wasNot called }
      }

      "Should be able to dispatch a workflow with annotation" {
        // when
        val deferred = client.dispatch(fooWorkflow::m)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName("foo"),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("bar"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow with annotation on parent" {
        // when
        val deferred = client.dispatch(fooWorkflow::annotated)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName("foo"),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("bar"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow from a parent interface" {
        // when
        val deferred = client.dispatch(fakeWorkflow::parent)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("parent"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow with meta" {
        // when
        val deferred = client.dispatch(fakeWorkflowWithMeta::m0)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("m0"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(meta),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow with tags" {
        // when
        val deferred = client.dispatch(fakeWorkflowWithTags::m0)
        // then
        workflowTagSlots.size shouldBe 2
        workflowTagSlots.toSet() shouldBe
            tags
                .map {
                  AddTagToWorkflow(
                      workflowName = WorkflowName(FakeWorkflow::class.java.name),
                      workflowTag = WorkflowTag(it),
                      workflowId = WorkflowId(deferred.id),
                      emitterName = clientNameTest,
                  )
                }
                .toSet()

        workflowEngineSlot.captured shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("m0"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                workflowTags = tags.map { WorkflowTag(it) }.toSet(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow with a one primitive as parameter" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m1, 0)
        // then
        workflowEngineSlot.isCaptured shouldBe true
        val msg = workflowEngineSlot.captured
        msg shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("m1"),
                methodParameters = MethodParameters.from(0),
                methodParameterTypes = MethodParameterTypes(listOf(Integer::class.java.name)),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow with two primitive parameters" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m3, 0, "a")
        // then
        workflowEngineSlot.isCaptured shouldBe true
        val msg = workflowEngineSlot.captured
        msg shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("m3"),
                methodParameters = MethodParameters.from(0, "a"),
                methodParameterTypes =
                MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a workflow with an interface as parameter" {
        // when
        val klass = FakeClass()
        val deferred = client.dispatch(fakeWorkflow::m4, klass)
        // then
        workflowEngineSlot.isCaptured shouldBe true
        val msg = workflowEngineSlot.captured

        msg shouldBe
            DispatchWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodName = MethodName("m4"),
                methodParameters = MethodParameters.from(klass),
                methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
                workflowTags = setOf(),
                workflowMeta = WorkflowMeta(),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to wait for a dispatched workflow" {
        val success = "success"

        // when
        val result = client.dispatch(fakeWorkflow::m3, 0, "a").await()

        // then
        result shouldBe success

        val msg = workflowEngineSlot.captured
        msg shouldBe
            WaitWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = msg.workflowId,
                methodRunId = MethodRunId.from(msg.workflowId),
                emitterName = clientNameTest,
            )

        // when waiting for a workflow, the consumer should be started
        verify { consumer.startClientConsumerAsync(any(), clientNameTest) }

        // restart a workflow
        client.dispatch(fakeWorkflow::m3, 0, "a").await()

        // the consumer should be started only once
        verify { consumer.startClientConsumerAsync(any(), any()) wasNot called }
      }

      "Should throw when calling a channel from a new workflow" {
        // when
        shouldThrow<InvalidChannelUsageException> { fakeWorkflow.channelString.send("a") }
      }

      "Should be able to send to a channel by id (sync)" {
        // when
        val id = UUID.randomUUID().toString()
        client.getWorkflowById(FakeWorkflow::class.java, id).channelString.send("a")
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowEngineSlot.captured as SendSignal
        msg shouldBe
            SendSignal(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                channelName = ChannelName("getChannelString"),
                signalId = msg.signalId,
                signalData = SignalData.from("a"),
                channelTypes = ChannelType.allFrom(String::class.java),
                emitterName = clientNameTest,
            )
      }

      "Should be able to emit to a channel by id (async)" {
        // when
        val id = UUID.randomUUID().toString()
        val w = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.dispatchAsync(w.channelString::send, "a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowEngineSlot.captured as SendSignal
        msg shouldBe
            SendSignal(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                channelName = ChannelName("getChannelString"),
                signalId = msg.signalId,
                signalData = SignalData.from("a"),
                channelTypes = ChannelType.allFrom(String::class.java),
                emitterName = clientNameTest,
            )
      }

      "Should be able to emit to a channel by tag (sync)" {
        val tag = "foo"
        // when
        client.getWorkflowByTag(FakeWorkflow::class.java, tag).channelString.send("a")
        // then
        val msg = workflowTagSlots[0] as SendSignalByTag
        msg shouldBe
            SendSignalByTag(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowTag = WorkflowTag(tag),
                channelName = ChannelName("getChannelString"),
                signalId = msg.signalId,
                signalData = SignalData.from("a"),
                channelTypes = ChannelType.allFrom(String::class.java),
                emitterWorkflowId = null,
                emitterName = clientNameTest,
            )
      }

      "Should be able to emit to a channel by tag (async)" {
        val tag = "foo"
        // when
        val w = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        client.dispatchAsync(w.channelString::send, "a").join()
        // then
        val msg = workflowTagSlots[0] as SendSignalByTag
        msg shouldBe
            SendSignalByTag(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowTag = WorkflowTag(tag),
                channelName = ChannelName("getChannelString"),
                signalId = msg.signalId,
                signalData = SignalData.from("a"),
                channelTypes = ChannelType.allFrom(String::class.java),
                emitterWorkflowId = null,
                emitterName = clientNameTest,
            )
      }

      "Should be able to send a complex Object to a channel" {
        // when
        val id = UUID.randomUUID().toString()
        val signal = FakeTaskImpl()
        client.getWorkflowById(FakeWorkflow::class.java, id).channelFakeTask.send(signal)
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowEngineSlot.captured as SendSignal
        msg shouldBe
            SendSignal(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                channelName = ChannelName("getChannelFakeTask"),
                signalId = msg.signalId,
                signalData = SignalData.from(signal),
                channelTypes =
                setOf(
                    ChannelType.from(FakeTaskImpl::class.java),
                    ChannelType.from(FakeTask::class.java),
                    ChannelType.from(FakeTaskParent::class.java),
                ),
                emitterName = clientNameTest,
            )
      }

      "Should be able to send a complex Object to a channel targeting a parent type" {
        // when
        val id = UUID.randomUUID().toString()
        val signal = FakeTaskImpl()
        client.getWorkflowById(FakeWorkflow::class.java, id).channelFakeTaskParent.send(signal)
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowEngineSlot.captured as SendSignal
        msg shouldBe
            SendSignal(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                channelName = ChannelName("getChannelFakeTaskParent"),
                signalId = msg.signalId,
                signalData = SignalData.from(signal),
                channelTypes =
                setOf(
                    ChannelType.from(FakeTaskImpl::class.java),
                    ChannelType.from(FakeTask::class.java),
                    ChannelType.from(FakeTaskParent::class.java),
                ),
                emitterName = clientNameTest,
            )
      }

      "Should be able to retry tasks of a workflow targeted by id (sync)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.retryTasks(workflow)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            RetryTasks(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                taskId = null,
                taskStatus = null,
                serviceName = null,
                emitterName = clientNameTest,
            )
      }

      "Should be able to retry tasks of a workflow targeted by id (async)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.retryTasksAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            RetryTasks(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                taskId = null,
                taskStatus = null,
                serviceName = null,
                emitterName = clientNameTest,
            )
      }

      "Should be able to complete timer of a workflow targeted by id (sync)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.completeTimers(workflow)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            CompleteTimers(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                methodRunId = null,
                emitterName = clientNameTest,
            )
      }

      "Should be able to complete timer of a workflow targeted by id (async)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.completeTimersAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            CompleteTimers(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                methodRunId = null,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a method on a workflow per id (async)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        val deferred = client.dispatch(workflow::m0)
        // then
        workflowEngineSlot.isCaptured shouldBe true
        val msg = workflowEngineSlot.captured
        msg shouldBe
            DispatchMethod(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                methodRunId = MethodRunId(deferred.id),
                methodName = MethodName("m0"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to dispatch a method on a workflow per Tag (async)" {
        // when
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        val deferred = client.dispatch(workflow::m0)
        println(deferred)
        // then
        workflowEngineSlot.isCaptured shouldBe false
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe
            DispatchMethodByTag(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowTag = WorkflowTag("foo"),
                methodRunId = (deferred as DeferredMethod).methodRunId,
                methodName = MethodName("m0"),
                methodParameters = MethodParameters(),
                methodParameterTypes = MethodParameterTypes(listOf()),
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = false,
                emitterName = clientNameTest,
            )
      }

      "Should be able to cancel workflow per id (sync)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.cancel(workflow)
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            CancelWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                methodRunId = null,
                reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                emitterName = clientNameTest,
            )
      }

      "Should be able to cancel workflow per id (async)" {
        // when
        val id = UUID.randomUUID().toString()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.cancelAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            CancelWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(id),
                methodRunId = null,
                reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                emitterName = clientNameTest,
            )
      }

      "Should be able to cancel workflow per tag (sync)" {
        // when
        val tag = "foo"
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        client.cancel(workflow)
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe
            CancelWorkflowByTag(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowTag = WorkflowTag(tag),
                reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                emitterWorkflowId = null,
                emitterName = clientNameTest,
            )
        workflowEngineSlot.isCaptured shouldBe false
      }

      "Should be able to cancel workflow per tag (async)" {
        // when
        val tag = "foo"
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        client.cancelAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe
            CancelWorkflowByTag(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowTag = WorkflowTag("foo"),
                reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                emitterWorkflowId = null,
                emitterName = clientNameTest,
            )
        workflowEngineSlot.isCaptured shouldBe false
      }

      "Should be able to cancel workflow just dispatched" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m0)
        deferred.cancel()
        // then
        workflowTagSlots.size shouldBe 0
        workflowEngineSlot.captured shouldBe
            CancelWorkflow(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
                methodRunId = null,
                reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                emitterName = clientNameTest,
            )
      }

      "Get workflow ids per tag" {
        val tag = "foo"
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        val workflowIds = client.getIds(workflow)
        // then
        workflowIds.size shouldBe 2
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe
            GetWorkflowIdsByTag(
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowTag = WorkflowTag("foo"),
                emitterName = clientNameTest,
            )
        workflowEngineSlot.isCaptured shouldBe false
      }

      "Wait a channel should throw" {
        shouldThrow<InvalidChannelUsageException> { client.await(fakeWorkflow.channelString) }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, UUID.randomUUID().toString())
        shouldThrow<InvalidStubException> { client.await(byId.channelString) }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> { client.await(byTag.channelString) }
      }

      "Wait a channel method should throw" {
        shouldThrow<InvalidChannelUsageException> {
          client.await(fakeWorkflow.channelString, UUID.randomUUID().toString())
        }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, UUID.randomUUID().toString())
        shouldThrow<InvalidStubException> {
          client.await(byId.channelString, UUID.randomUUID().toString())
        }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> {
          client.await(byTag.channelString, UUID.randomUUID().toString())
        }
      }

      "Retry a channel should throw" {
        shouldThrow<InvalidChannelUsageException> {
          client.retryWorkflowTask(fakeWorkflow.channelString)
        }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, UUID.randomUUID().toString())
        shouldThrow<InvalidStubException> { client.retryWorkflowTask(byId.channelString) }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> { client.retryWorkflowTask(byTag.channelString) }
      }

      "Cancel a channel should throw" {
        shouldThrow<InvalidChannelUsageException> { client.cancel(fakeWorkflow.channelString) }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, UUID.randomUUID().toString())
        shouldThrow<InvalidStubException> { client.cancel(byId.channelString) }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> { client.cancel(byTag.channelString) }
      }

      "Get ids from channel should throw" {
        shouldThrow<InvalidChannelUsageException> { client.getIds(fakeWorkflow.channelString) }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, UUID.randomUUID().toString())
        shouldThrow<InvalidStubException> { client.getIds(byId.channelString) }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> { client.getIds(byTag.channelString) }
      }

      "Should throw when using an instance" {
        val fake = FakeWorkflowImpl()
        shouldThrow<InvalidStubException> { client.dispatch(fake::m0) }
      }
    },
)
