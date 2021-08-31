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
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIds
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.exceptions.clients.CanNotApplyOnNewWorkflowStubException
import io.infinitic.exceptions.clients.CanNotReuseWorkflowStubException
import io.infinitic.exceptions.clients.MultipleMethodCallsException
import io.infinitic.exceptions.clients.NoMethodCallException
import io.infinitic.exceptions.clients.SuspendMethodNotSupportedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging
import java.util.UUID

private val taskTagSlots = mutableListOf<TaskTagEngineMessage>()
private val taskSlot = slot<TaskEngineMessage>()
private val workflowTagSlots = mutableListOf<WorkflowTagEngineMessage>()
private val workflowSlot = slot<WorkflowEngineMessage>()

class ClientWorkflow : AbstractInfiniticClient() {
    override val sendingScope = CoroutineScope(Dispatchers.IO)
    override val clientName = ClientName("clientTest")
    override val sendToTaskTagEngine = mockSendToTaskTagEngine(this, taskTagSlots)
    override val sendToTaskEngine = mockSendToTaskEngine(this, taskSlot)
    override val sendToWorkflowTagEngine = mockSendToWorkflowTagEngine(this, workflowTagSlots)
    override val sendToWorkflowEngine = mockSendToWorkflowEngine(this, workflowSlot)
    override fun close() {}
}

class ClientWorkflowTests : StringSpec({
    val client = ClientWorkflow()

    beforeTest {
        client.join()
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowSlot.clear()
    }

    "Should throw when re-using a stub" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<CanNotReuseWorkflowStubException> {
            client.async(fakeWorkflow) { m1() }
            client.async(fakeWorkflow) { m1() }
        }
    }

    "Should throw when calling 2 methods" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<MultipleMethodCallsException> {
            client.async(fakeWorkflow) { m1(); m1() }
        }
    }

    "Should throw when not calling any method" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<NoMethodCallException> {
            client.async(fakeWorkflow) { }
        }
    }

    "Should throw when retrying new stub" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<CanNotApplyOnNewWorkflowStubException> {
            client.retry(fakeWorkflow)
        }
    }

    "Should throw when canceling new stub" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<CanNotApplyOnNewWorkflowStubException> {
            client.cancel(fakeWorkflow)
        }
    }

    "Should throw when using a suspend method" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<SuspendMethodNotSupportedException> {
            fakeWorkflow.suspendedMethod()
        }
    }

    "Should be able to dispatch a workflow without parameter" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1() }.join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with annotation" {
        // when
        val fooWorkflow = client.newWorkflow<FooWorkflow>()
        val deferred = client.async(fooWorkflow) { m() }.join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName("foo"),
            methodName = MethodName("bar"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with annotation on parent" {
        // when
        val fooWorkflow = client.newWorkflow<FooWorkflow>()
        val deferred = client.async(fooWorkflow) { annotated() }.join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName("foo"),
            methodName = MethodName("bar"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow from a parent interface" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { parent() }.join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("parent"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with Java syntax" {
        // when
        val fakeWorkflow = client.newWorkflow(FakeWorkflow::class.java)
        val deferred = client.async(fakeWorkflow) { m1() }.join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with options and meta" {
        // when
        val options = TestFactory.random<WorkflowOptions>()
        val meta = mapOf(
            "foo" to TestFactory.random<ByteArray>(),
            "bar" to TestFactory.random<ByteArray>()
        )
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>(options = options, meta = meta)
        val deferred = client.async(fakeWorkflow) { m1() }.join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = options,
            workflowMeta = WorkflowMeta(meta)
        )
    }

    "Should be able to dispatch a workflow with tags" {
        // when
        val tags = setOf("foo", "bar")
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>(tags = tags)
        val deferred = client.async(fakeWorkflow) { m1() }.join()
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
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(WorkflowTag("foo"), WorkflowTag("bar")),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with a primitive as parameter" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1(0) }.join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Integer::class.java.name)),
            methodParameters = MethodParameters.from(0),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with multiple method definition" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1("a") }.join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodParameters = MethodParameters.from("a"),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with multiple parameters" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1(0, "a") }.join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with an interface as parameter" {
        // when
        val klass = FakeClass()
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1(klass) }.join()
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured

        msg shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = false,
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            methodParameters = MethodParameters.from(klass),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow synchronously" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val result = fakeWorkflow.m1(0, "a")
        // then
        result shouldBe "success"

        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = client.clientName,
            clientWaiting = true,
            workflowId = msg.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            workflowTags = setOf(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to wait for a workflow just dispatched" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1(0, "a") }.join()
        val result = deferred.await()
        // then
        result shouldBe "success"
    }

    "Should be able to emit to a channel by id" {
        // when
        val id = UUID.randomUUID()
        val fakeWorkflow = client.getWorkflow(FakeWorkflow::class.java, id)
        fakeWorkflow.channel.send("a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelEventId = msg.channelEventId,
            channelName = ChannelName("getChannel"),
            channelEvent = ChannelEvent.from("a"),
            channelEventTypes = ChannelEventType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel by tag" {
        val tag = "foo"
        // when
        val fakeWorkflow = client.getWorkflow(FakeWorkflow::class.java, tag)
        fakeWorkflow.channel.send("a").join()
        // then
        val msg = workflowTagSlots[0] as SendToChannelPerTag
        msg shouldBe SendToChannelPerTag(
            workflowTag = WorkflowTag(tag),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            clientWaiting = true,
            channelEventId = msg.channelEventId,
            channelName = ChannelName("getChannel"),
            channelEvent = ChannelEvent.from("a"),
            channelEventTypes = ChannelEventType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel after workflow dispatch" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1(0, "a") }.join()
        fakeWorkflow.channel.send("a").join()
        // then
        workflowTagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = client.clientName,
            channelEventId = msg.channelEventId,
            channelName = ChannelName("getChannel"),
            channelEvent = ChannelEvent.from("a"),
            channelEventTypes = ChannelEventType.allFrom(String::class.java)
        )
    }

    "Should be able to cancel workflow per id" {
        // when
        val id = UUID.randomUUID()
        val fakeWorkflow = client.getWorkflow(FakeWorkflow::class.java, id)
        client.cancel(fakeWorkflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name)
        )
    }

    "Should be able to cancel workflow per tag" {
        // when
        val fakeWorkflow = client.getWorkflow<FakeWorkflow>("foo")
        client.cancel(fakeWorkflow).join()
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe CancelWorkflowPerTag(
            workflowTag = WorkflowTag("foo"),
            workflowName = WorkflowName(FakeWorkflow::class.java.name)
        )
        workflowSlot.isCaptured shouldBe false
    }

    "Should be able to cancel workflow per tag with output" {
        // when
        val fakeWorkflow = client.getWorkflow<FakeWorkflow>("foo")
        client.cancel(fakeWorkflow).join()
        // then
        workflowTagSlots.size shouldBe 1
        workflowTagSlots[0] shouldBe CancelWorkflowPerTag(
            workflowTag = WorkflowTag("foo"),
            workflowName = WorkflowName(FakeWorkflow::class.java.name)
        )
        workflowSlot.isCaptured shouldBe false
    }

    "Should be able to cancel workflow just dispatched" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val deferred = client.async(fakeWorkflow) { m1() }.join()
        client.cancel(fakeWorkflow).join()
        // then
        workflowTagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(deferred.id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name)
        )
    }

    "get task ids par name and workflow" {
        val workflowIds = client.getWorkflowIds<FakeWorkflow>("foo")
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
