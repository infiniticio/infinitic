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
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.exceptions.SendToChannelFailed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.coroutineScope

class ClientWorkflowTests : StringSpec({
    val taskSlot = slot<TaskEngineMessage>()
    val workflowSlot = slot<WorkflowEngineMessage>()
    val clientOutput = MockClientOutput(taskSlot, workflowSlot)
    val client = Client(clientOutput)
    clientOutput.client = client
    val id = TestFactory.random<String>()
    val fakeWorkflow = client.workflow(FakeWorkflow::class.java)
    val fakeWorkflowId = client.workflow(FakeWorkflow::class.java, id)

    beforeTest {
        taskSlot.clear()
        workflowSlot.clear()
    }

    "Should be able to dispatch a workflow without parameter" {
        // when
        val workflowId = WorkflowId(client.async(fakeWorkflow) { m1() })
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
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

    "Should be able to dispatch a workflow with a primitive as parameter" {
        // when
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

    "Should be able to emit to a channel asynchronously" {
        // when
        val sendId = ChannelEventId(client.async(fakeWorkflowId.ch) { send("a") })

        // then
        val msg = workflowSlot.captured
        msg shouldBe SendToChannel(
            clientName = clientOutput.clientName,
            clientWaiting = false,
            channelEventId = sendId,
            workflowId = msg.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            channelName = ChannelName("getCh"),
            channelEvent = ChannelEvent.from("a"),
            channelEventTypes = ChannelEventType.allFrom(String::class.java)
        )
    }

    "Should be able to emit to a channel synchronously" {
        // when
        coroutineScope {
            fakeWorkflowId.ch.send("a")
        }

        // then
        val msg = workflowSlot.captured as SendToChannel
        msg shouldBe SendToChannel(
            clientName = clientOutput.clientName,
            clientWaiting = true,
            channelEventId = msg.channelEventId,
            workflowId = msg.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            channelName = ChannelName("getCh"),
            channelEvent = ChannelEvent.from("a"),
            channelEventTypes = ChannelEventType.allFrom(String::class.java)
        )
    }

    "Should throw an exception when emit to a channel synchronously to non-existing workflow" {
        // when
        coroutineScope {
            shouldThrow<SendToChannelFailed> {
                fakeWorkflowId.ch.send("unknown")
            }
        }
    }

    // TODO: add tests for options

    // TODO: add tests for meta

    // TODO: add tests for error cases
})
