/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.client

import io.infinitic.client.samples.FakeClass
import io.infinitic.client.samples.FakeInterface
import io.infinitic.client.samples.FakeWorkflow
import io.infinitic.client.samples.FooWorkflow
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalType
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIds
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.exceptions.clients.ChannelUsedOnNewWorkflowException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private val taskTagSlots = CopyOnWriteArrayList<TaskTagEngineMessage>() // multithreading update
private val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagEngineMessage>() // multithreading update
private val taskSlot = slot<TaskEngineMessage>()
private val workflowSlot = slot<WorkflowEngineMessage>()

class ClientWorkflow : InfiniticClient() {
    override val sendingScope = CoroutineScope(Dispatchers.IO)
    override val clientName = ClientName("clientTest")
    override val sendToTaskTagEngine = mockSendToTaskTagEngine(this, taskTagSlots)
    override val sendToTaskEngine = mockSendToTaskEngine(this, taskSlot)
    override val sendToWorkflowTagEngine = mockSendToWorkflowTagEngine(this, workflowTagSlots)
    override val sendToWorkflowEngine = mockSendToWorkflowEngine(this, workflowSlot)
}

class ClientWorkflowTests : StringSpec({
    val client = ClientWorkflow()

    val fakeWorkflow = client.newWorkflowStub(FakeWorkflow::class.java)
    val fooWorkflow = client.newWorkflowStub(FooWorkflow::class.java)

    beforeTest {
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowSlot.clear()
    }

    afterTest {
        client.join()
    }

    "Should be able to dispatch a workflow" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m0)().join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with annotation" {
        // when
        val deferred = client.dispatch(fooWorkflow::m)().join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName("foo"),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("bar"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with annotation on parent" {
        // when
        val deferred = client.dispatch(fooWorkflow::annotated)().join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName("foo"),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("bar"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow from a parent interface" {
        // when
        val deferred = client.dispatch(fakeWorkflow::parent)().join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("parent"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with options and meta" {
        // when
        val options = TestFactory.random<WorkflowOptions>()
        val meta: Map<String, ByteArray> = mapOf(
            "foo" to TestFactory.random(),
            "bar" to TestFactory.random()
        )
        val deferred = client.dispatch(fakeWorkflow::m0, options = options, meta = meta)().join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowTags = setOf(),
            workflowOptions = options,
            workflowMeta = WorkflowMeta(meta)
        )
    }

    "Should be able to dispatch a workflow with tags" {
        // when
        val tags = setOf("foo", "bar")
        val deferred = client.dispatch(fakeWorkflow::m0, tags = tags)().join()
        // then
        workflowTagSlots.size shouldBe 2
        workflowTagSlots.toSet() shouldBe setOf(
            AddWorkflowTag(
                workflowTag = WorkflowTag("foo"),
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
            ),
            AddWorkflowTag(
                workflowTag = WorkflowTag("bar"),
                workflowName = WorkflowName(FakeWorkflow::class.java.name),
                workflowId = WorkflowId(deferred.id),
            )
        )
        workflowSlot.captured shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowTags = setOf(WorkflowTag("foo"), WorkflowTag("bar")),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with a one primitive as parameter" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m1)(0).join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Integer::class.java.name)),
            methodParameters = MethodParameters.from(0),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with two primitive parameters" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m3)(0, "a").join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("m3"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with an interface as parameter" {
        // when
        val klass = FakeClass()
        val deferred = client.dispatch(fakeWorkflow::m4)(klass).join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured

        msg shouldBe DispatchWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = false,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = MethodName("m4"),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            methodParameters = MethodParameters.from(klass),
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to wait for a dispatched workflow" {
        // when
        val result = client.dispatch(fakeWorkflow::m3)(0, "a").await()
        // then
        result shouldBe "success"

        val msg = workflowSlot.captured
        msg shouldBe WaitWorkflow(
            workflowId = msg.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName
        )
    }

    "Should throw when calling a channel from a new workflow" {
        // when
        shouldThrow<ChannelUsedOnNewWorkflowException> {
            fakeWorkflow.channel.send("a")
        }
    }

    "Should be able to emit to a channel by id synchronously" {
        // when
        val id = UUID.randomUUID()
        val instance = client.getInstanceStub(fakeWorkflow, id)
        instance.channel.send("a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelSignalId = msg.channelSignalId,
            channelName = ChannelName("getChannel"),
            channelSignal = ChannelSignal.from("a"),
            channelSignalTypes = ChannelSignalType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel by id asynchronously" {
        // when
        val id = UUID.randomUUID()
        val instance = client.getInstanceStub(fakeWorkflow, id)
        client.dispatch(instance.channel::send)("a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelSignalId = msg.channelSignalId,
            channelName = ChannelName("getChannel"),
            channelSignal = ChannelSignal.from("a"),
            channelSignalTypes = ChannelSignalType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel by id" {
        // when
        val id = UUID.randomUUID()
        val instance = client.getInstanceStub(fakeWorkflow, id)
        instance.channel.send("a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelSignalId = msg.channelSignalId,
            channelName = ChannelName("getChannel"),
            channelSignal = ChannelSignal.from("a"),
            channelSignalTypes = ChannelSignalType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel by tag" {
        val tag = "foo"
        // when
        val instance = client.getInstanceStub(fakeWorkflow, tag)
        instance.channel.send("a").join()
        // then
        val msg = workflowTagSlots[0] as SendToChannelPerTag
        msg shouldBe SendToChannelPerTag(
            workflowTag = WorkflowTag(tag),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelSignalId = msg.channelSignalId,
            channelName = ChannelName("getChannel"),
            channelSignal = ChannelSignal.from("a"),
            channelSignalTypes = ChannelSignalType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel after workflow dispatch" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m3)(0, "a").join()
        client.getInstanceStub(fakeWorkflow, deferred.id).channel.send("a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelSignalId = msg.channelSignalId,
            channelName = ChannelName("getChannel"),
            channelSignal = ChannelSignal.from("a"),
            channelSignalTypes = ChannelSignalType.allFrom(String::class.java)
        )
    }

    "Should be able to cancel workflow per id - syntax 1" {
        // when
        val id = UUID.randomUUID()
        client.cancel(fakeWorkflow, id).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
    }

    "Should be able to cancel workflow per id - syntax 2" {
        // when
        val id = UUID.randomUUID()
        client.cancel(fakeWorkflow, id).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
    }

    "Should be able to cancel workflow per tag - syntax 1" {
        // when
        client.cancel(fakeWorkflow, "foo").join()
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe CancelWorkflowPerTag(
            workflowTag = WorkflowTag("foo"),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
        workflowSlot.isCaptured shouldBe false
    }

    "Should be able to cancel workflow per tag - syntax 2" {
        // when
        client.cancel(fakeWorkflow, "foo").join()
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe CancelWorkflowPerTag(
            workflowTag = WorkflowTag("foo"),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
        workflowSlot.isCaptured shouldBe false
    }

    "Should be able to cancel workflow just dispatched" {
        // when
        val deferred = client.dispatch(fakeWorkflow::m0)().join()
        deferred.cancel().join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
    }

    "get task ids par name and workflow" {
        val workflowIds = client.getIds(fakeWorkflow, "foo")
        // then
        workflowIds.size shouldBe 2
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe GetWorkflowIds(
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowTag = WorkflowTag("foo"),
            clientName = ClientName("clientTest")
        )
        workflowSlot.isCaptured shouldBe false
    }
})
