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

package io.infinitic.inMemory

import io.infinitic.clients.getWorkflow
import io.infinitic.clients.newWorkflow
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.CanceledDeferredException
import io.infinitic.exceptions.clients.FailedDeferredException
import io.infinitic.inMemory.tasks.TaskA
import io.infinitic.inMemory.tasks.TaskAImpl
import io.infinitic.inMemory.workflows.Obj1
import io.infinitic.inMemory.workflows.Obj2
import io.infinitic.inMemory.workflows.WorkflowA
import io.infinitic.inMemory.workflows.WorkflowAImpl
import io.infinitic.inMemory.workflows.WorkflowB
import io.infinitic.inMemory.workflows.WorkflowBImpl
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import io.infinitic.exceptions.workflows.CanceledDeferredException as CanceledInWorkflowException
import io.infinitic.exceptions.workflows.FailedDeferredException as FailedInWorkflowException

private val taskExecutorRegister = TaskExecutorRegisterImpl().apply {
    registerTask(TaskA::class.java.name) { TaskAImpl() }
    registerWorkflow(WorkflowA::class.java.name) { WorkflowAImpl() }
    registerWorkflow(WorkflowB::class.java.name) { WorkflowBImpl() }
}

val client = InfiniticClient(taskExecutorRegister, "client: inMemory")

internal class InMemoryTests : StringSpec({

    lateinit var workflowA: WorkflowA
    lateinit var workflowATagged: WorkflowA
    lateinit var workflowAMeta: WorkflowA
    lateinit var workflowB: WorkflowB

    beforeTest {
        workflowA = client.newWorkflow()
        workflowATagged = client.newWorkflow(setOf("foo", "bar"))
        workflowAMeta = client.newWorkflow(meta = mapOf("foo" to "bar".toByteArray()))
        workflowB = client.newWorkflow()
    }

    afterSpec {
        client.close()
    }

    "empty Workflow" {
        val deferred = client.async(workflowA) { empty() }
        val result = deferred.await()

        result shouldBe "void"
    }

    "get id from context" {
        val deferred = client.async(workflowA) { context1() }
        val result = deferred.await()

        result shouldBe deferred.id
    }

    "get tags from context" {
        val deferred = client.async(workflowATagged) { context2() }
        val result = deferred.await()

        result shouldBe setOf("foo", "bar")
    }

    "get meta from context" {
        val deferred = client.async(workflowAMeta) { context3() }
        val result = deferred.await()

        result shouldBe WorkflowMeta(mapOf("foo" to "bar".toByteArray()))
    }

    "get workflow id from task context" {
        val deferred = client.async(workflowA) { context4() }
        val result = deferred.await()

        result shouldBe deferred.id
    }

    "get workflow name from task context" {
        val deferred = client.async(workflowA) { context5() }
        val result = deferred.await()

        result shouldBe WorkflowA::class.java.name
    }

    "Simple Sequential Workflow" {
        val deferred = client.async(workflowA) { seq1() }
        val result = deferred.await()

        result shouldBe "123"
    }

    "Wait for synchronous Workflow" {
        val result = workflowA.seq1()

        result shouldBe "123"
    }

    "Wait for asynchronous Workflow" {
        val deferred = client.async(workflowA) { seq1() }
        val result = deferred.await()

        result shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        val deferred = client.async(workflowA) { seq2() }
        val result = deferred.await()

        result shouldBe "23ba"
    }

    "Sequential Workflow with an async branch" {
        val deferred = client.async(workflowA) { seq3() }
        val result = deferred.await()

        result shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        val deferred = client.async(workflowA) { seq4() }
        val result = deferred.await()

        result shouldBe "23bac"
    }

    "Test Deferred methods" {
        val deferred = client.async(workflowA) { deferred1() }
        val result = deferred.await()

        result shouldBe "truefalsefalsetrue"
    }

    "Or step with 3 async tasks" {
        val deferred = client.async(workflowA) { or1() }
        val result = deferred.await()

        result shouldBeIn listOf("ba", "dc", "fe")
    }

    "Combined And/Or step with 3 async tasks" {
        val deferred = client.async(workflowA) { or2() }
        val result = deferred.await()

        result shouldBeIn listOf(listOf("ba", "dc"), "fe")
    }

    "Or step with 3 async tasks through list" {
        val deferred = client.async(workflowA) { or3() }
        val result = deferred.await()

        result shouldBeIn listOf("ba", "dc", "fe")
    }

    "Or step with Status checking" {
        val deferred = client.async(workflowA) { or4() }
        val result = deferred.await()

        result shouldBe "baba"
    }

    "And step with 3 async tasks" {
        val deferred = client.async(workflowA) { and1() }
        val result = deferred.await()

        result shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through list" {
        val deferred = client.async(workflowA) { and2() }
        val result = deferred.await()

        result shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through large list" {
        val deferred = client.async(workflowA) { and3() }
        val result = deferred.await()

        result shouldBe MutableList(1_00) { "ba" }
    }

    "Inline task" {
        val deferred = client.async(workflowA) { inline1(7) }
        val result = deferred.await()

        result shouldBe "2 * 7 = 14"
    }

    "Inline task with asynchronous task inside" {
        val deferred = client.async(workflowA) { inline2(21) }
        val result = deferred.await()

        result shouldBe "2 * 21 = 42"
    }

    "Inline task with synchronous task inside" {
        val deferred = client.async(workflowA) { inline3(14) }
        val e = shouldThrow<FailedDeferredException> { deferred.await() }
    }

    "Sequential Child Workflow" {
        val deferred = client.async(workflowA) { child1() }
        val result = deferred.await()

        result shouldBe "-abc-"
    }

    "Asynchronous Child Workflow" {
        val deferred = client.async(workflowA) { child2() }
        val result = deferred.await()

        result shouldBe "21abc21"
    }

    "Nested Child Workflow" {
        val deferred = client.async(workflowB) { factorial(14) }
        val result = deferred.await()

        result shouldBe 87178291200
    }

    "Check prop1" {
        val deferred = client.async(workflowA) { prop1() }
        val result = deferred.await()

        result shouldBe "ac"
    }

    "Check prop2" {
        val deferred = client.async(workflowA) { prop2() }
        val result = deferred.await()

        result shouldBe "acbd"
    }

    "Check prop3" {
        val deferred = client.async(workflowA) { prop3() }
        val result = deferred.await()

        result shouldBe "acbd"
    }

    "Check prop4" {
        val deferred = client.async(workflowA) { prop4() }
        val result = deferred.await()

        result shouldBe "acd"
    }

    "Check prop5" {
        val deferred = client.async(workflowA) { prop5() }
        val result = deferred.await()

        result shouldBe "adbc"
    }

    "Check prop6" {
        val deferred = client.async(workflowA) { prop6() }
        val result = deferred.await()

        result shouldBe "abab"
    }

    "Check prop7" {
        val deferred = client.async(workflowA) { prop7() }
        val result = deferred.await()

        result shouldBe "acbd"
    }

    "Check prop6 sync" {
        val result = workflowA.prop6()

        result shouldBe "abab"
    }

    "Check multiple sync" {
        val result1 = workflowA.seq1()
        val result2 = client.newWorkflow<WorkflowA>().prop1()

        result1 shouldBe "123"
        result2 shouldBe "ac"
    }

    "Waiting for event, sent after dispatched" {
        val deferred = client.async(workflowA) { channel1() }
        workflowA.channelA.send("test")
        val result = deferred.await()

        result shouldBe "test"
    }

    "Waiting for event, sent by id" {
        val deferred = client.async(workflowA) { channel1() }
        client.getWorkflow<WorkflowA>(deferred.id).channelA.send("test")
        val result = deferred.await()

        result shouldBe "test"
    }

    "Waiting for event, sent by tag" {
        val deferred = client.async(workflowATagged) { channel1() }
        client.getWorkflow<WorkflowA>("foo").channelA.send("test")
        val result = deferred.await()

        result shouldBe "test"
    }

    "Waiting for event, sent to the right channel" {
        val deferred = client.async(workflowA) { channel2() }
        client.getWorkflow<WorkflowA>(deferred.id).channelA.send("test")
        val result = deferred.await()

        result shouldBe "test"
    }

    "Waiting for event but sent to the wrong channel" {
        val deferred = client.async(workflowA) { channel2() }
        client.getWorkflow<WorkflowA>(deferred.id).channelB.send("test")
        val result = deferred.await()

        result::class.java.name shouldBe Instant::class.java.name
    }

    "Sending event before waiting for it prevents catching" {
        val deferred = client.async(workflowA) { channel3() }
        client.getWorkflow<WorkflowA>(deferred.id).channelA.send("test")
        val result = deferred.await()

        result::class.java.name shouldBe Instant::class.java.name
    }

    "Waiting for Obj event" {
        val obj1 = Obj1("foo", 42)
        val deferred = client.async(workflowA) { channel4() }
        workflowA.channelObj.send(obj1)
        val result = deferred.await()

        result shouldBe obj1
    }

    "Waiting for filtered event using jsonPath only" {
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        val deferred = client.async(workflowA) { channel4bis() }
        workflowA.channelObj.send(obj1a)
        delay(50)
        workflowA.channelObj.send(obj1b)
        val result = deferred.await()

        result shouldBe obj1b
    }

    "Waiting for filtered event using using jsonPath and criteria" {
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        val deferred = client.async(workflowA) { channel4ter() }
        workflowA.channelObj.send(obj1a)
        delay(50)
        workflowA.channelObj.send(obj1b)
        val result = deferred.await()

        result shouldBe obj1b
    }

    "Waiting for event of specific type" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val deferred = client.async(workflowA) { channel5() }
        workflowA.channelObj.send(obj2)
        delay(50)
        workflowA.channelObj.send(obj1)
        val result = deferred.await()

        result shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath only" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        val deferred = client.async(workflowA) { channel5bis() }
        workflowA.channelObj.send(obj3)
        delay(50)
        workflowA.channelObj.send(obj2)
        delay(50)
        workflowA.channelObj.send(obj1)
        val result = deferred.await()

        result shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath and criteria" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        val deferred = client.async(workflowA) { channel5ter() }
        client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj3)
        delay(50)
        client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj2)
        delay(50)
        client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj1)
        val result = deferred.await()

        result shouldBe obj1
    }

    "Waiting for 2 events of specific types presented in wrong order" {
        val obj1 = Obj1("foo", 6)
        val obj2 = Obj2("bar", 7)
        val deferred = client.async(workflowA) { channel6() }
        client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj2)
        delay(50)
        workflowA.channelObj.send(obj1)
        val result = deferred.await()

        result shouldBe "foobar42"
    }

    "Tag should be added and deleted after completion" {
        val deferred = client.async(workflowATagged) { channel1() }
        val id = deferred.id
        // checks id has been added to tag storage
        client.workflowTagStorage.getWorkflowIds(WorkflowTag("foo"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe true
        client.workflowTagStorage.getWorkflowIds(WorkflowTag("bar"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe true
        workflowATagged.channelA.send("test")
        val result = deferred.await()

        result shouldBe "test"
        delay(50)
        client.workflowTagStorage.getWorkflowIds(WorkflowTag("foo"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe false
        client.workflowTagStorage.getWorkflowIds(WorkflowTag("bar"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe false
    }

    "Canceling async workflow" {
        val deferred = client.async(workflowA) { channel1() }
        client.cancel(workflowA)

        delay(100)
        client.workflowStateStorage.getState(WorkflowId(deferred.id)) shouldBe null
    }

    "Canceling sync workflow" {
        launch { client.cancel(workflowA) }
        shouldThrow<CanceledDeferredException> { workflowA.channel1() }
    }

    "Canceling sync workflow with deferred" {
        val deferred = client.async(workflowA) { channel1() }
        launch { client.cancel(workflowA) }
        shouldThrow<CanceledDeferredException> { deferred.await() }

        // check output
        client.workflowStateStorage.getState(WorkflowId(deferred.id)) shouldBe null
    }

    "try/catch a failing task" {
        val result = workflowA.failing1()

        result shouldBe "ko"
    }

    "failing task on main path should throw" {
        val e = shouldThrow<FailedDeferredException> { workflowA.failing2() }

        e.causeError?.errorName shouldBe FailedInWorkflowException::class.java.name
        e.causeError?.name shouldBe TaskA::class.java.name
    }

    "failing async task on main path should not throw" {
        val result = workflowA.failing2a()

        result shouldBe 100
    }

    "failing task not on main path should not throw" {
        val result = workflowA.failing3()

        result shouldBe 100
    }

    "failing instruction not on main path should not throw" {
        val result = workflowA.failing3b()

        result shouldBe 100
    }

    "Canceling task on main path should throw " {
        val e = shouldThrow<FailedDeferredException> { workflowA.failing4() }

        e.causeError?.errorName shouldBe CanceledInWorkflowException::class.java.name
        e.causeError?.name shouldBe TaskA::class.java.name
    }

    "Canceling task not on main path should not throw " {
        val result = workflowA.failing5()

        result shouldBe 100
    }

    "Canceling child workflow on main path should throw" {
        val e = shouldThrow<FailedDeferredException> { workflowB.cancelChild1() }

        e.causeError?.errorName shouldBe CanceledInWorkflowException::class.java.name
        e.causeError?.name shouldBe WorkflowA::class.java.name
    }

    "Canceling child workflow not on main path should not throw" {
        val result = workflowB.cancelChild2()

        result shouldBe 100
    }

    "Failure in child workflow on main path should throw exception" {
        val e = shouldThrow<FailedDeferredException> { workflowA.failing6() }

        e.causeError?.errorName shouldBe FailedInWorkflowException::class.java.name
        e.causeError?.name shouldBe WorkflowA::class.java.name

        e.causeError?.errorCause?.errorName shouldBe FailedInWorkflowException::class.java.name
        e.causeError?.errorCause?.name shouldBe TaskA::class.java.name
    }

    "Failure in child workflow not on main path should not throw" {
        val result = workflowA.failing7()

        result shouldBe 100
    }

    "retry a failed task should work" {
    }

    "retry a caught failed task should not throw" {
    }
})
