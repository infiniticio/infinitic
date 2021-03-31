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
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.AddWorkflowTag
import io.infinitic.common.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.tags.messages.SendToChannelPerTag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.exceptions.CanNotReuseWorkflowStub
import io.infinitic.exceptions.CanNotUseNewWorkflowStub
import io.infinitic.exceptions.MultipleMethodCalls
import io.infinitic.exceptions.NoMethodCall
import io.infinitic.exceptions.SuspendMethodNotSupported
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class ClientWorkflowTests : StringSpec({
    val tagSlots = mutableListOf<TagEngineMessage>()
    val taskSlot = slot<TaskEngineMessage>()
    val workflowSlot = slot<WorkflowEngineMessage>()
    val client = Client()
    val clientOutput = mockClientOutput(client, tagSlots, taskSlot, workflowSlot)
    client.clientOutput = clientOutput
    val tag = TestFactory.random<String>()

    beforeTest {
        tagSlots.clear()
        workflowSlot.clear()
    }

    "Should throw when re-using a stub" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<CanNotReuseWorkflowStub> {
            client.async(fakeWorkflow) { m1() }
            client.async(fakeWorkflow) { m1() }
        }
    }

    "Should throw when calling 2 methods" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<MultipleMethodCalls> {
            client.async(fakeWorkflow) { m1(); m1() }
        }
    }

    "Should throw when not calling any method" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<NoMethodCall> {
            client.async(fakeWorkflow) { }
        }
    }

    "Should throw when retrying new stub" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<CanNotUseNewWorkflowStub> {
            client.retry(fakeWorkflow)
        }
    }

    "Should throw when canceling new stub" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<CanNotUseNewWorkflowStub> {
            client.cancel(fakeWorkflow)
        }
    }

    "Should throw when using a suspend method" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        shouldThrow<SuspendMethodNotSupported> {
            fakeWorkflow.suspendedMethod()
        }
    }

    "Should be able to dispatch a workflow without parameter" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1() })
        // then
        tagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with Java syntax" {
        // when
        val fakeWorkflow = client.newWorkflow(FakeWorkflow::class.java)
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1() })
        // then
        tagSlots.size shouldBe 0
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentMethodRunId = null,
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

        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1() })
        // then
        tagSlots.size shouldBe options.tags.size
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = options,
            workflowMeta = WorkflowMeta(meta)
        )
    }

    "Should be able to dispatch a workflow without tags" {
        // when
        val options = WorkflowOptions(tags = setOf("foo", "bar"))
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>(options = options)
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1() })
        // then
        tagSlots.size shouldBe 2
        tagSlots[0] shouldBe AddWorkflowTag(
            tag = Tag("foo"),
            name = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = workflowId
        )
        tagSlots[1] shouldBe AddWorkflowTag(
            tag = Tag("bar"),
            name = WorkflowName(FakeWorkflow::class.java.name),
            workflowId = workflowId
        )
        workflowSlot.captured shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = options,
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with a primitive as parameter" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1(0) })
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Integer::class.java.name)),
            methodParameters = MethodParameters.from(0),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with multiple method definition" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1("a") })
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodParameters = MethodParameters.from("a"),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with multiple parameters" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1(0, "a") })
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with an interface as parameter" {
        // when
        val klass = FakeClass()
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1(klass) })
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured

        msg shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            workflowId = workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            methodParameters = MethodParameters.from(klass),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow synchronously" {
        var result: String
        // when
        coroutineScope {
            val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
            result = fakeWorkflow.m1(0, "a")
        }
        // then
        result shouldBe "success"

        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            clientName = clientOutput.clientName,
            clientWaiting = true,
            workflowId = msg.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to emit to a channel by id" {
        // when
        val id = UUID.randomUUID()
        coroutineScope {
            val fakeWorkflow = client.getWorkflow(FakeWorkflow::class.java, id)
            fakeWorkflow.channel.send("a")
        }

        // then
        tagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = clientOutput.clientName,
            channelEventId = msg.channelEventId,
            channelName = ChannelName("getChannel"),
            channelEvent = ChannelEvent.from("a"),
            channelEventTypes = ChannelEventType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel by tag" {
        // when
        coroutineScope {
            val fakeWorkflow = client.getWorkflow(FakeWorkflow::class.java, tag)
            fakeWorkflow.channel.send("a")
        }

        // then
        val msg = tagSlots[0] as SendToChannelPerTag
        msg shouldBe SendToChannelPerTag(
            tag = Tag(tag),
            name = WorkflowName(FakeWorkflow::class.java.name),
            clientName = clientOutput.clientName,
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
        val id = client.async(fakeWorkflow) { m1(0, "a") }
        fakeWorkflow.channel.send("a")

        // then
        tagSlots.size shouldBe 0
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            clientName = clientOutput.clientName,
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
        client.cancel(fakeWorkflow)
        // then
        tagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowReturnValue = MethodReturnValue.from(null)
        )
    }

    "Should be able to cancel workflow per id with output" {
        val output = TestFactory.random<String>()
        // when
        val id = UUID.randomUUID()
        val fakeWorkflow = client.getWorkflow<FakeWorkflow>(id)
        client.cancel(fakeWorkflow, output)
        // then
        tagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowReturnValue = MethodReturnValue.from(output)
        )
    }

    "Should be able to cancel workflow per tag" {
        // when
        val fakeWorkflow = client.getWorkflow<FakeWorkflow>("foo")
        client.cancel(fakeWorkflow)
        // then
        tagSlots.size shouldBe 1
        tagSlots[0] shouldBe CancelWorkflowPerTag(
            tag = Tag("foo"),
            name = WorkflowName(FakeWorkflow::class.java.name),
            workflowReturnValue = MethodReturnValue.from(null)
        )
        workflowSlot.isCaptured shouldBe false
    }

    "Should be able to cancel workflow per tag with output" {
        val output = TestFactory.random<String>()
        // when
        val fakeWorkflow = client.getWorkflow<FakeWorkflow>("foo")
        client.cancel(fakeWorkflow, output)
        // then
        tagSlots.size shouldBe 1
        tagSlots[0] shouldBe CancelWorkflowPerTag(
            tag = Tag("foo"),
            name = WorkflowName(FakeWorkflow::class.java.name),
            workflowReturnValue = MethodReturnValue.from(output)
        )
        workflowSlot.isCaptured shouldBe false
    }

    "Should be able to cancel workflow just dispatched" {
        // when
        val fakeWorkflow = client.newWorkflow<FakeWorkflow>()
        val id = client.async(fakeWorkflow) { m1() }

        client.cancel(fakeWorkflow)
        // then
        tagSlots.size shouldBe 0
        workflowSlot.captured shouldBe CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            workflowReturnValue = MethodReturnValue.from(null)
        )
    }
})
