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

import io.infinitic.clients.config.InfiniticClientConfig
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
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.later
import io.infinitic.common.fixtures.methodParametersFrom
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.CompleteDelegatedTask
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.utils.IdGenerator
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
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
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.exceptions.WorkflowTimedOutException
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.inMemory.InMemoryInfiniticConsumer
import io.infinitic.inMemory.InMemoryInfiniticProducer
import io.infinitic.transport.config.InMemoryTransportConfig
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
import java.util.concurrent.CopyOnWriteArrayList

private val taskTagSlots = CopyOnWriteArrayList<ServiceTagMessage>()
private val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagEngineMessage>()
private val taskSlot = slot<ServiceExecutorMessage>()
private val workflowCmdSlot = slot<WorkflowCmdMessage>()
private val delaySlot = slot<MillisDuration>()

private val clientNameTest = ClientName("clientTest")
private val emitterNameTest = EmitterName("clientTest")
private fun tagResponse() {
  workflowTagSlots.forEach {
    if (it is GetWorkflowIdsByTag) {
      val workflowIdsByTag = WorkflowIdsByTag(
          recipientName = ClientName(client.name),
          workflowName = it.workflowName,
          workflowTag = it.workflowTag,
          workflowIds = setOf(WorkflowId(), WorkflowId()),
          emitterName = EmitterName("mockk"),
      )
      later { client.handle(workflowIdsByTag) }
    }
  }
}

private fun engineResponse() {
  val msg = workflowCmdSlot.captured
  if (msg is DispatchWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
    val methodCompleted = MethodCompleted(
        recipientName = ClientName(client.name),
        workflowId = msg.workflowId,
        workflowMethodId = WorkflowMethodId.from(msg.workflowId),
        methodReturnValue = MethodReturnValue.from("success", null),
        emitterName = EmitterName("mockk"),
    )
    later { client.handle(methodCompleted) }
  }
}

internal val mockedProducer = mockk<InMemoryInfiniticProducer> {
  every { name } returns "$clientNameTest"
  coEvery {
    internalSendTo(capture(taskTagSlots), ServiceTagEngineTopic)
  } answers { }
  coEvery {
    internalSendTo(capture(workflowTagSlots), WorkflowTagEngineTopic)
  } answers { tagResponse() }
  coEvery {
    internalSendTo(capture(workflowCmdSlot), WorkflowStateCmdTopic)
  } answers { engineResponse() }
}

internal val mockedConsumer = mockk<InMemoryInfiniticConsumer> {
  coEvery { start(any<Subscription<*>>(), "$clientNameTest", any(), any(), any()) } just Runs
}

internal val mockedTransport = mockk<InMemoryTransportConfig> {
  every { consumer } returns mockedConsumer
  every { producer } returns mockedProducer
  every { shutdownGracePeriodSeconds } returns 5.0
}

internal val infiniticClientConfig = InfiniticClientConfig(transport = mockedTransport)

private val client = InfiniticClient(infiniticClientConfig)

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
        workflowCmdSlot.captured shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
            clientWaiting = false,
            emitterName = emitterNameTest,
            emittedAt = null,
        )

        // when asynchronously dispatching a workflow, the consumer should not be started
        coVerify(exactly = 0) {
          mockedConsumer.start(
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
        workflowCmdSlot.captured shouldBe DispatchWorkflow(
            workflowName = WorkflowName("foo"),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("bar"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
        workflowCmdSlot.captured shouldBe DispatchWorkflow(
            workflowName = WorkflowName("foo"),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("bar"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
        workflowCmdSlot.captured shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("parent"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
        workflowCmdSlot.captured shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(meta),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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

        workflowCmdSlot.captured shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m0"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            workflowTags = tags.map { WorkflowTag(it) }.toSet(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
        msg shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m1"),
            methodParameters = methodParametersFrom(0),
            methodParameterTypes = MethodParameterTypes(listOf(Integer::class.java.name)),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
        msg shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m3"),
            methodParameters = methodParametersFrom(0, "a"),
            methodParameterTypes =
            MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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

        msg shouldBe DispatchWorkflow(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(deferred.id),
            methodName = MethodName("m4"),
            methodParameters = MethodArgs(
                listOf(
                    SerializedData.encode(
                        klass,
                        FakeInterface::class.java,
                        null,
                    ),
                ),
            ),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            workflowTags = setOf(),
            workflowMeta = WorkflowMeta(),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
        )

        // when waiting for a workflow, the consumer should be started
        coVerify {
          mockedConsumer.start(
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
          mockedConsumer.start(
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
            signalData = SignalData.from("a", String::class.java),
            channelTypes = ChannelType.allFrom(String::class.java),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            signalData = SignalData.from("a", String::class.java),
            channelTypes = ChannelType.allFrom(String::class.java),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            emitterName = emitterNameTest,
            emittedAt = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            signalData = SignalData.from("a", String::class.java),
            channelTypes = ChannelType.allFrom(String::class.java),
            parentWorkflowId = null,
            emitterName = emitterNameTest,
            emittedAt = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            signalData = SignalData.from("a", String::class.java),
            channelTypes = ChannelType.allFrom(String::class.java),
            parentWorkflowId = null,
            emitterName = emitterNameTest,
            emittedAt = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            signalData = SignalData.from(signal, FakeService::class.java),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            signalData = SignalData.from(signal, FakeServiceParent::class.java),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
        msg shouldBe DispatchMethod(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = WorkflowId(id),
            workflowMethodId = WorkflowMethodId(deferred.id),
            workflowMethodName = MethodName("m0"),
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            methodParameters = MethodArgs(),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodTimeout = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            emitterName = emitterNameTest,
            emittedAt = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            emitterName = emitterNameTest,
            emittedAt = null,
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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
            requester = ClientRequester(clientName = ClientName.from(emitterNameTest)),
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

      "Should be able to complete a task" {
        // when
        val taskId = TestFactory.random<String>()
        // complex result
        val result = TestFactory.random<WorkflowEngineEnvelope>()
        client.completeDelegatedTask(FakeService::class.java.name, taskId, result)
        // then
        taskTagSlots.size shouldBe 1
        val msg = taskTagSlots.first() as CompleteDelegatedTask
        msg shouldBe CompleteDelegatedTask(
            returnValue = MethodReturnValue.from(result, null),
            messageId = msg.messageId,
            serviceName = ServiceName(FakeService::class.java.name),
            taskId = TaskId(taskId),
            emitterName = emitterNameTest,
        )
      }
    },
)
