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

import io.infinitic.clients.deferred.ExistingDeferredWorkflow
import io.infinitic.clients.samples.FakeClass
import io.infinitic.clients.samples.FakeInterface
import io.infinitic.clients.samples.FakeService
import io.infinitic.clients.samples.FakeServiceImpl
import io.infinitic.clients.samples.FakeServiceParent
import io.infinitic.clients.samples.FakeWorkflow
import io.infinitic.clients.samples.FakeWorkflowImpl
import io.infinitic.clients.samples.FooWorkflow
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.later
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethodWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.exceptions.WorkflowTimedOutException
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.InvalidStubException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

private val taskTagSlots = CopyOnWriteArrayList<ServiceTagMessage>() // multithreading update
private val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagMessage>() // multithreading update
private val taskSlot = slot<ServiceExecutorMessage>()
private val workflowCmdSlot = slot<WorkflowCmdMessage>()
private val delaySlot = slot<MillisDuration>()

private val clientNameTest = ClientName("clientTest")
private val emitterNameTest = EmitterName("clientTest")
private fun completed() = CompletableFuture.completedFuture(Unit)
private fun tagResponse(): CompletableFuture<Unit> {
  workflowTagSlots.forEach {
    if (it is GetWorkflowIdsByTag) {
      val workflowIdsByTag = WorkflowIdsByTag(
          recipientName = ClientName(client.name),
          workflowName = it.workflowName,
          workflowTag = it.workflowTag,
          workflowIds = setOf(WorkflowId(), WorkflowId()),
          emitterName = EmitterName("mockk"),
      )
      later { client.handle(workflowIdsByTag, MillisInstant.now()) }
    }
  }
  return completed()
}

private fun engineResponse(): CompletableFuture<Unit> {
  val msg = workflowCmdSlot.captured
  if (msg is DispatchNewWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
    val methodCompleted = MethodCompleted(
        recipientName = ClientName(client.name),
        workflowId = msg.workflowId,
        workflowMethodId = WorkflowMethodId.from(msg.workflowId),
        methodReturnValue = ReturnValue.from("success"),
        emitterName = EmitterName("mockk"),
    )
    later { client.handle(methodCompleted, MillisInstant.now()) }
  }
  return completed()
}

private val producerAsync = mockk<InfiniticProducerAsync> {
  every { producerName } returns "$clientNameTest"
  coEvery { capture(workflowTagSlots).sendToAsync(WorkflowTagTopic) } answers { tagResponse() }
  coEvery { capture(workflowCmdSlot).sendToAsync(WorkflowCmdTopic) } answers { engineResponse() }
}

private val consumerAsync = mockk<InfiniticConsumerAsync> {
  coEvery { start(any<Subscription<*>>(), "$clientNameTest", any(), any(), any()) } just Runs
}

private val client = InfiniticClient(consumerAsync, producerAsync)

internal class InfiniticClientTests : StringSpec(
    {
      val meta: Map<String, ByteArray> = mapOf(
          "foo" to TestFactory.random(),
          "bar" to TestFactory.random(),
      )
      val tags = setOf("foo", "bar")

      val fakeWorkflow = client.newWorkflow(FakeWorkflow::class.java)
      val fooWorkflow = client.newWorkflow(FooWorkflow::class.java)

      val fakeWorkflowWithMeta = client.newWorkflow(FakeWorkflow::class.java, meta = meta)
      val fakeWorkflowWithTags = client.newWorkflow(FakeWorkflow::class.java, tags = tags)

      beforeTest {
        delaySlot.clear()
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowCmdSlot.clear()
      }

      "Should be able to dispatch a workflow" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m0)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )

        // when asynchronously dispatching a workflow, the consumer should not be started
        coVerify(exactly = 0) {
          consumerAsync.start(
              MainSubscription(ClientTopic),
              "$clientNameTest",
              any(),
              any(),
              1,
          )
        }
      }

      "Should be able to dispatch a workflow with annotation" {
        // when
        val deferred = client.dispatch(fooWorkflow::m)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName("foo"),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("bar"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow with annotation on parent" {
        // when
        val deferred = client.dispatch(fooWorkflow::annotated)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName("foo"),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("bar"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow from a parent interface" {
        // when
        val deferred = client.dispatch(fakeWorkflow::parent)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("parent"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow with meta" {
        // when
        val deferred = client.dispatch(fakeWorkflowWithMeta::m0)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(meta),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow with tags" {
        // when
        val deferred = client.dispatch(fakeWorkflowWithTags::m0)
        // then
        workflowTagSlots.size shouldBe 2
        workflowTagSlots.toSet() shouldBe tags.map {
          AddTagToWorkflow(
              workflowName = WorkflowName(FakeWorkflow::class.java.name),
              workflowTag = WorkflowTag(it),
              workflowId = WorkflowId(deferred.id),
              emitterName = emitterNameTest,
              emittedAt = null,
          )
        }.toSet()

        workflowCmdSlot.captured shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = tags.map { WorkflowTag(it) }.toSet(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow with a one primitive as parameter" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m1, 0)
        // then
        workflowCmdSlot.isCaptured shouldBe true
        val msg = workflowCmdSlot.captured
        msg shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m1"),
            methodParameters = MethodParameters.from(0),
            methodParameterTypes = MethodParameterTypes(listOf(Integer::class.java.name)),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow with two primitive parameters" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m3, 0, "a")
        // then
        workflowCmdSlot.isCaptured shouldBe true
        val msg = workflowCmdSlot.captured
        msg shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m3"),
            methodParameters = MethodParameters.from(0, "a"),
            methodParameterTypes =
            MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a workflow with an interface as parameter" {
        // when
        val klass = FakeClass()
        val deferred = client.dispatch(fakeWorkflow::m4, klass)
        // then
        workflowCmdSlot.isCaptured shouldBe true
        val msg = workflowCmdSlot.captured

        msg shouldBe DispatchNewWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m4"),
            methodParameters = MethodParameters.from(klass),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to wait for a dispatched workflow" {
        val success = "success"

        // when
        val result = client.dispatch(fakeWorkflow::m3, 0, "a").await()

        // then
        result shouldBe success

        val msg = workflowCmdSlot.captured
        msg shouldBe WaitWorkflow(
            workflowMethodId = WorkflowMethodId.from(msg.workflowId),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = msg.workflowId,
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )

        // when waiting for a workflow, the consumer should be started
        coVerify {
          consumerAsync.start(
              MainSubscription(ClientTopic),
              "$clientNameTest",
              any(),
              any(),
              1,
          )
        }

        // restart a workflow
        client.dispatch(fakeWorkflow::m3, 0, "a").await()

        // the consumer should be started only once
        coVerify(exactly = 1) {
          consumerAsync.start(
              MainSubscription(ClientTopic),
              "$clientNameTest",
              any(),
              any(),
              1,
          )
        }
      }

      "Should throw a WorkflowTimedOutException when waiting for a workflow more than timeout" {
        // when triggered asynchronously
        shouldThrow<WorkflowTimedOutException> {
          client.dispatch(fakeWorkflow::timeout).await()
        }

        // when triggered synchronously
        shouldThrow<WorkflowTimedOutException> {
          fakeWorkflow.timeout()
        }
      }

      "Should throw when calling a channel from a new workflow" {
        // when
        shouldThrow<InvalidChannelUsageException> { fakeWorkflow.channelString.send("a") }
      }

      "Should be able to send to a channel by id (sync)" {
        // when
        val id = IdGenerator.next()
        client.getWorkflowById(FakeWorkflow::class.java, id).channelString.send("a")
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowCmdSlot.captured as SendSignal
        msg shouldBe SendSignal(
            channelName = ChannelName("getChannelString"),
            signalId = msg.signalId,
            signalData = SignalData.from("a"),
            channelTypes = ChannelType.allFrom(String::class.java),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to send to a channel by id (async)" {
        // when
        val id = IdGenerator.next()
        val w = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.dispatchAsync(w.channelString::send, "a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowCmdSlot.captured as SendSignal
        msg shouldBe SendSignal(
            channelName = ChannelName("getChannelString"),
            signalId = msg.signalId,
            signalData = SignalData.from("a"),
            channelTypes = ChannelType.allFrom(String::class.java),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to emit to a channel by tag (sync)" {
        val tag = "foo"
        // when
        client.getWorkflowByTag(FakeWorkflow::class.java, tag).channelString.send("a")
        // then
        val msg = workflowTagSlots[0] as SendSignalByTag
        msg shouldBe SendSignalByTag(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag(tag),
            channelName = ChannelName("getChannelString"),
            signalId = msg.signalId,
            signalData = SignalData.from("a"),
            channelTypes = ChannelType.allFrom(String::class.java),
            parentWorkflowId = null,
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to emit to a channel by tag (async)" {
        val tag = "foo"
        // when
        val w = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        client.dispatchAsync(w.channelString::send, "a").join()
        // then
        val msg = workflowTagSlots[0] as SendSignalByTag
        msg shouldBe SendSignalByTag(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag(tag),
            channelName = ChannelName("getChannelString"),
            signalId = msg.signalId,
            signalData = SignalData.from("a"),
            channelTypes = ChannelType.allFrom(String::class.java),
            parentWorkflowId = null,
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to send a complex Object to a channel" {
        // when
        val id = IdGenerator.next()
        val signal = FakeServiceImpl()
        client.getWorkflowById(FakeWorkflow::class.java, id).channelFakeTask.send(signal)
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowCmdSlot.captured as SendSignal
        msg shouldBe SendSignal(
            channelName = ChannelName("getChannelFakeTask"),
            signalId = msg.signalId,
            signalData = SignalData.from(signal),
            channelTypes =
            setOf(
                ChannelType.from(FakeServiceImpl::class.java),
                ChannelType.from(FakeService::class.java),
                ChannelType.from(FakeServiceParent::class.java),
            ),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to send a complex Object to a channel targeting a parent type" {
        // when
        val id = IdGenerator.next()
        val signal = FakeServiceImpl()
        client.getWorkflowById(FakeWorkflow::class.java, id).channelFakeServiceParent.send(signal)
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowCmdSlot.captured as SendSignal
        msg shouldBe SendSignal(
            channelName = ChannelName("getChannelFakeServiceParent"),
            signalId = msg.signalId,
            signalData = SignalData.from(signal),
            channelTypes =
            setOf(
                ChannelType.from(FakeServiceImpl::class.java),
                ChannelType.from(FakeService::class.java),
                ChannelType.from(FakeServiceParent::class.java),
            ),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to retry tasks of a workflow targeted by id (sync)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.retryTasks(workflow)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe RetryTasks(
            taskId = null,
            taskStatus = null,
            serviceName = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to retry tasks of a workflow targeted by id (async)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.retryTasksAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe RetryTasks(
            taskId = null,
            taskStatus = null,
            serviceName = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to complete timer of a workflow targeted by id (sync)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.completeTimers(workflow)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe CompleteTimers(
            workflowMethodId = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to complete timer of a workflow targeted by id (async)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.completeTimersAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe CompleteTimers(
            workflowMethodId = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to dispatch a method on a workflow per id (async)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        val deferred = client.dispatch(workflow::m0)
        // then
        workflowCmdSlot.isCaptured shouldBe true
        val msg = workflowCmdSlot.captured
        msg shouldBe DispatchMethodWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            workflowMethodId = WorkflowMethodId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to dispatch a method on a workflow per Tag (async)" {
        // when
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        val deferred = client.dispatch(workflow::m0)
        // then
        workflowCmdSlot.isCaptured shouldBe false
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe DispatchMethodByTag(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag("foo"),
            workflowMethodId = (deferred as ExistingDeferredWorkflow).workflowMethodId,
            methodName = MethodName("m0"),
            methodParameters = MethodParameters(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodTimeout = null,
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )
      }

      "Should be able to cancel workflow per id (sync)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.cancel(workflow)
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe CancelWorkflow(
            cancellationReason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            workflowMethodId = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to cancel workflow per id (async)" {
        // when
        val id = IdGenerator.next()
        val workflow = client.getWorkflowById(FakeWorkflow::class.java, id)
        client.cancelAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe CancelWorkflow(
            cancellationReason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            workflowMethodId = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Should be able to cancel workflow per tag (sync)" {
        // when
        val tag = "foo"
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        client.cancel(workflow)
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe CancelWorkflowByTag(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag(tag),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            emitterWorkflowId = null,
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
        workflowCmdSlot.isCaptured shouldBe false
      }

      "Should be able to cancel workflow per tag (async)" {
        // when
        val tag = "foo"
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        client.cancelAsync(workflow).join()
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe CancelWorkflowByTag(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag("foo"),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            emitterWorkflowId = null,
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
        workflowCmdSlot.isCaptured shouldBe false
      }

      "Should be able to cancel workflow just dispatched" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m0)
        deferred.cancel()
        // then
        workflowTagSlots.size shouldBe 0
        workflowCmdSlot.captured shouldBe CancelWorkflow(
            cancellationReason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            workflowMethodId = null,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
      }

      "Get workflow ids per tag" {
        val tag = "foo"
        val workflow = client.getWorkflowByTag(FakeWorkflow::class.java, tag)
        val workflowIds = client.getIds(workflow)
        // then
        workflowIds.size shouldBe 2
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe GetWorkflowIdsByTag(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag("foo"),
            emitterName = emitterNameTest,
            emittedAt = null,
        )
        workflowCmdSlot.isCaptured shouldBe false
      }

      "Retry a channel should throw" {
        shouldThrow<InvalidChannelUsageException> {
          client.retryWorkflowTask(fakeWorkflow.channelString)
        }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, IdGenerator.next())
        shouldThrow<InvalidStubException> { client.retryWorkflowTask(byId.channelString) }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> { client.retryWorkflowTask(byTag.channelString) }
      }

      "Cancel a channel should throw" {
        shouldThrow<InvalidChannelUsageException> { client.cancel(fakeWorkflow.channelString) }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, IdGenerator.next())
        shouldThrow<InvalidStubException> { client.cancel(byId.channelString) }

        val byTag = client.getWorkflowByTag(FakeWorkflow::class.java, "foo")
        shouldThrow<InvalidStubException> { client.cancel(byTag.channelString) }
      }

      "Get ids from channel should throw" {
        shouldThrow<InvalidChannelUsageException> { client.getIds(fakeWorkflow.channelString) }

        val byId = client.getWorkflowById(FakeWorkflow::class.java, IdGenerator.next())
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
