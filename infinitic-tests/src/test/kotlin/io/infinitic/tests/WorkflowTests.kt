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

package io.infinitic.tests

// import com.github.valfirst.slf4jtest.TestLoggerFactory
import io.infinitic.common.fixtures.later
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.exceptions.clients.CanceledDeferredException
import io.infinitic.exceptions.clients.FailedDeferredException
import io.infinitic.factory.InfiniticClientFactory
import io.infinitic.factory.InfiniticWorkerFactory
import io.infinitic.tests.tasks.TaskA
import io.infinitic.tests.workflows.Obj1
import io.infinitic.tests.workflows.Obj2
import io.infinitic.tests.workflows.WorkflowA
import io.infinitic.tests.workflows.WorkflowAnnotated
import io.infinitic.tests.workflows.WorkflowB
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.config.configuration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
// import uk.org.lidalia.slf4jext.Level
import java.time.Instant
import kotlin.concurrent.thread
import io.infinitic.exceptions.workflows.CanceledDeferredException as CanceledInWorkflowException
import io.infinitic.exceptions.workflows.FailedDeferredException as FailedInWorkflowException

internal class WorkflowTests : StringSpec({

    // each test should not be longer than 10s
    configuration.timeout = 10000

    val client = autoClose(InfiniticClientFactory.fromConfigResource("/pulsar.yml"))
    val worker = autoClose(InfiniticWorkerFactory.fromConfigResource("/pulsar.yml"))

    val taskA = client.newTaskStub(TaskA::class.java)
    val workflowA = client.newWorkflowStub(WorkflowA::class.java)
    val workflowATagged = client.newWorkflowStub(WorkflowA::class.java, tags = setOf("foo", "bar"))
    val workflowAMeta = client.newWorkflowStub(WorkflowA::class.java, meta = mapOf("foo" to "bar".toByteArray()))
    val workflowB = client.newWorkflowStub(WorkflowB::class.java)
    val workflowAnnotated = client.newWorkflowStub(WorkflowAnnotated::class.java)

//    val logger = TestLoggerFactory.getTestLogger(WorkflowTests::class.java)

    beforeSpec {
        // print info level log - despite using inMemory TestLoggerFactory implementation
//        TestLoggerFactory.getInstance().printLevel = Level.DEBUG

        thread { worker.start() }
    }

    beforeTest {
        // note that log events of previous test can continue to fill at this stage, due to asynchronous calls
//        TestLoggerFactory.clearAll()
//        logger.clear()

        worker.storageFlush()
    }

    suspend fun expectDiscardingForHavingNullState(expected: Boolean = false) {
        // When processed by Pulsar, logs are always flowing - this test can not be written that way
//        if (! PulsarInfiniticClient::class.isInstance(client)) {
//            // make sure all events are recorded
//            delay(1000)
//            // check that workflow state is not deleted too early
//            TestLoggerFactory.getAllLoggingEvents()
//                .filter { it.level == Level.INFO }
//                .map { "${it.timestamp} ${it.message}" }
//                .joinToString("\n")
// //                 .also { println(it) }
//                .contains(WorkflowEngine.NO_STATE_DISCARDING_REASON) shouldBe expected
//        }
    }

    "empty Workflow" {
        workflowA.empty() shouldBe "void"
    }

    "run task from parent interface" {
        workflowA.parent() shouldBe "ok"
    }

    "run childWorkflow from parent interface" {
        workflowA.wparent() shouldBe "ok"
    }

    "get id from context" {
        val deferred = client.dispatch(workflowA::context1)().join()

        deferred.await() shouldBe deferred.id
    }

    "get tags from context" {
        workflowATagged.context2() shouldBe setOf("foo", "bar")
    }

    "get meta from context" {
        workflowAMeta.context3() shouldBe WorkflowMeta(mapOf("foo" to "bar".toByteArray()))
    }

    "get workflow id from task context" {
        val deferred = client.dispatch(workflowA::context4)().join()

        deferred.await() shouldBe deferred.id
    }

    "get workflow name from task context" {
        workflowA.context5() shouldBe WorkflowA::class.java.name
    }

    "get task tags from task context" {
        workflowA.context6() shouldBe setOf("foo", "bar")
    }

    "get task meta from task context" {
        workflowA.context7() shouldBe TaskMeta(mapOf("foo" to "bar".toByteArray()))
    }

    "Simple Sequential Workflow" {
        workflowA.seq1() shouldBe "123"
    }

    "Wait for a dispatched Workflow" {
        val deferred = client.dispatch(workflowA::seq1)().join()

        deferred.await() shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        workflowA.seq2() shouldBe "23ba"
    }

    "Sequential Workflow with an async branch" {
        workflowA.seq3() shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        workflowA.seq4() shouldBe "23bac"
    }

    "Workflow waiting 2 deferred in wrong order" {
        workflowA.seq5() shouldBe 600
    }

    "Workflow waiting 2 deferred in wrong order followed by a step" {
        workflowA.seq6() shouldBe 600
    }

    "Test Deferred methods" {
        workflowA.deferred1() shouldBe "truefalsefalsetrue"
    }

    "Or step with 3 async tasks" {
        workflowA.or1() shouldBeIn listOf("ba", "dc", "fe")
    }

    "Combined And/Or step with 3 async tasks" {
        workflowA.or2() shouldBeIn listOf(listOf("ba", "dc"), "fe")
    }

    "Or step with 3 async tasks through list" {
        workflowA.or3() shouldBeIn listOf("ba", "dc", "fe")
    }

    "Or step with Status checking" {
        workflowA.or4() shouldBe "baba"

        // TODO CHECK THIS ONE
        // expectDiscardingForHavingNullState(true)
    }

    "And step with 3 async tasks" {
        workflowA.and1() shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through list" {
        workflowA.and2() shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through large list" {
        workflowA.and3() shouldBe MutableList(1_00) { "ba" }
    }

    "Inline task" {
        workflowA.inline1(7) shouldBe "2 * 7 = 14"
    }

    "Inline task with asynchronous task inside" {
        workflowA.inline2(21) shouldBe "2 * 21 = 42"
    }

    "Inline task with synchronous task inside" {
        shouldThrow<FailedDeferredException> { workflowA.inline3(14) }
    }

    "Sequential Child Workflow" {
        workflowA.child1() shouldBe "-abc-"
    }

    "Asynchronous Child Workflow" {
        workflowA.child2() shouldBe "21abc21"
    }

    "Nested Child Workflow" {
        workflowB.factorial(14) shouldBe 87178291200
    }

    "Check prop1" {
        workflowA.prop1() shouldBe "ac"

        // should not discard state before completing the async branch
        // TODO check this case
//        expectDiscardingForHavingNullState(false)
    }

    "Check prop2" {
        workflowA.prop2() shouldBe "acbd"
    }

    "Check prop3" {
        workflowA.prop3() shouldBe "acbd"
    }

    "Check prop4" {
        workflowA.prop4() shouldBe "acd"
    }

    "Check prop5" {
        workflowA.prop5() shouldBe "adbc"
    }

    "Check prop6" {
        workflowA.prop6() shouldBe "abab"

        expectDiscardingForHavingNullState()
    }

    "Check prop7" {
        workflowA.prop7() shouldBe "acbd"
    }

    "Check multiple sync" {
        val result1 = workflowA.seq1()
        val result2 = workflowA.prop1()

        result1 shouldBe "123"
        result2 shouldBe "ac"
    }

    "Waiting for event, sent after dispatched" {
        val deferred = client.dispatch(workflowA::channel1)().join()

//        later { workflowA.channelA.send("test") }
        later { client.getInstanceStub(workflowA, deferred.id).channelA.send("test") }

        deferred.await() shouldBe "test"
    }

    "Waiting for event, sent by id" {
        val deferred = client.dispatch(workflowA::channel1)().join()

        later { client.getInstanceStub(workflowA, deferred.id).channelA.send("test") }

        deferred.await() shouldBe "test"
    }

    "Waiting for event, sent by tag" {
        val deferred = client.dispatch(workflowATagged::channel1)().join()

        later { client.getInstanceStub(workflowA, "foo").channelA.send("test") }

        deferred.await() shouldBe "test"
    }

    "Waiting for event, sent to the right channel" {
        val deferred = client.dispatch(workflowA::channel2)().join()

        later { client.getInstanceStub(workflowA, deferred.id).channelA.send("test") }

        deferred.await() shouldBe "test"
    }

    "Waiting for event but sent to the wrong channel" {
        val deferred = client.dispatch(workflowA::channel2)().join()

        later { client.getInstanceStub(workflowA, deferred.id).channelB.send("test") }

        deferred.await()::class.java.name shouldBe Instant::class.java.name
    }

    "Sending event before waiting for it prevents catching" {
        val deferred = client.dispatch(workflowA::channel3)().join()

        later { client.getInstanceStub(workflowA, deferred.id).channelA.send("test") }

        deferred.await()::class.java.name shouldBe Instant::class.java.name
    }

    "Waiting for Obj event" {
        val obj1 = Obj1("foo", 42)
        val deferred = client.dispatch(workflowA::channel4)().join()

        later { client.getInstanceStub(workflowA, deferred.id).channelObj.send(obj1) }

        deferred.await() shouldBe obj1
    }

    "Waiting for filtered event using jsonPath only" {
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        val deferred = client.dispatch(workflowA::channel4bis)().join()

        later {
            val instance = client.getInstanceStub(workflowA, deferred.id)
            instance.channelObj.send(obj1a)
            instance.channelObj.send(obj1b)
        }

        deferred.await() shouldBe obj1b
    }

    "Waiting for filtered event using using jsonPath and criteria" {
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        val deferred = client.dispatch(workflowA::channel4ter)().join()

        later {
            val instance = client.getInstanceStub(workflowA, deferred.id)
            instance.channelObj.send(obj1a)
            instance.channelObj.send(obj1b)
        }

        deferred.await() shouldBe obj1b
    }

    "Waiting for event of specific type" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val deferred = client.dispatch(workflowA::channel5)().join()

        later {
            val instance = client.getInstanceStub(workflowA, deferred.id)
            instance.channelObj.send(obj2)
            instance.channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath only" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        val deferred = client.dispatch(workflowA::channel5bis)().join()

        later {
            val instance = client.getInstanceStub(workflowA, deferred.id)
            instance.channelObj.send(obj3)
            instance.channelObj.send(obj2)
            instance.channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath and criteria" {
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        val deferred = client.dispatch(workflowA::channel5ter)().join()

        later {
            val instance = client.getInstanceStub(workflowA, deferred.id)
            instance.channelObj.send(obj3)
            instance.channelObj.send(obj2)
            instance.channelObj.send(obj1)
        }

        deferred.await() shouldBe obj1
    }

    "Waiting for 2 events of specific types presented in wrong order" {
        val obj1 = Obj1("foo", 6)
        val obj2 = Obj2("bar", 7)
        val deferred = client.dispatch(workflowA::channel6)().join()

        later {
            val instance = client.getInstanceStub(workflowA, deferred.id)
            instance.channelObj.send(obj2)
            instance.channelObj.send(obj1)
        }

        deferred.await() shouldBe "foobar42"
    }

    "Cancelling async workflow" {
        val deferred = client.dispatch(workflowA::channel1)().join()

        later { client.cancel(workflowA, deferred.id) }

        shouldThrow<CanceledDeferredException> { deferred.await() }
    }

    "Cancelling workflow" {
        val deferred = client.dispatch(workflowA::channel1)().join()

        later { client.cancel(workflowA, deferred.id) }

        shouldThrow<CanceledDeferredException> { deferred.await() }
    }

    "try/catch a failing task" {
        workflowA.failing1() shouldBe "ko"
    }

    "failing task on main path should throw" {
        val e = shouldThrow<FailedDeferredException> { workflowA.failing2() }

        e.causeError?.errorName shouldBe FailedInWorkflowException::class.java.name
        e.causeError?.whereName shouldBe TaskA::class.java.name
    }

    "failing async task on main path should not throw" {
        workflowA.failing2a() shouldBe 100
    }

    "failing task not on main path should not throw" {
        workflowA.failing3() shouldBe 100

        expectDiscardingForHavingNullState()
    }

    "failing instruction not on main path should not throw" {
        workflowA.failing3b() shouldBe 100

        expectDiscardingForHavingNullState()
    }

    "Cancelling task on main path should throw " {
        val e = shouldThrow<FailedDeferredException> { workflowA.failing4() }

        e.causeError?.errorName shouldBe CanceledInWorkflowException::class.java.name
        e.causeError?.whereName shouldBe TaskA::class.java.name
    }

    "Cancelling task not on main path should not throw " {
        workflowA.failing5() shouldBe 100
    }

    "Cancelling child workflow on main path should throw" {
        val e = shouldThrow<FailedDeferredException> { workflowB.cancelChild1() }

        e.causeError?.errorName shouldBe CanceledInWorkflowException::class.java.name
        e.causeError?.whereName shouldBe WorkflowA::class.java.name
    }

    "Cancelling child workflow not on main path should not throw" {
        workflowB.cancelChild2() shouldBe 100
    }

    "Failure in child workflow on main path should throw exception" {
        val e = shouldThrow<FailedDeferredException> { workflowA.failing6() }

        e.causeError?.errorName shouldBe FailedInWorkflowException::class.java.name
        e.causeError?.whereName shouldBe WorkflowA::class.java.name

        e.causeError?.errorCause?.errorName shouldBe FailedInWorkflowException::class.java.name
        e.causeError?.errorCause?.whereName shouldBe TaskA::class.java.name
    }

    "Failure in child workflow not on main path should not throw" {
        workflowA.failing7() shouldBe 100
    }

    "Retry a failed task from client should restart a workflow" {
        val deferred = client.dispatch(workflowA::failing8)().join()

        val e = shouldThrow<FailedDeferredException> { deferred.await() }

        e.causeError?.whereName shouldBe TaskA::class.java.name

        client.retry(taskA, e.causeError?.whereId!!).join()

        deferred.await() shouldBe "ok"
    }

    "retry a caught failed task should not throw and influence workflow" {
        workflowA.failing9() shouldBe true
    }

    "properties should be correctly set after a deferred cancellation" {
        workflowA.failing10() shouldBe "ok"
    }

    "child workflow is canceled when parent workflow is canceled - tag are also added and deleted" {
        val deferred = client.dispatch(workflowATagged::cancel1)().join()

        delay(1000)
        val size = client.getIds(workflowA, "foo").size

        client.cancel(workflowA, deferred.id).join()

        delay(1000)
        client.getIds(workflowA, "foo").size shouldBe size - 2
    }

    "Tag should be added then deleted after completion" {
        val deferred = client.dispatch(workflowATagged::channel1)().join()

        // delay is necessary to be sure that tag engine has processed
        delay(500)
        client.getIds(workflowA, "foo").contains(deferred.id) shouldBe true

        // complete workflow
        client.getInstanceStub(workflowA, "foo").channelA.send("").join()

        // delay is necessary to be sure that tag engine has processed
        delay(500)
        client.getIds(workflowA, "foo").contains(deferred.id) shouldBe false
    }

    "Annotated Workflow" {
        val result = workflowAnnotated.foo("")

        result shouldBe "abc"
    }
})
