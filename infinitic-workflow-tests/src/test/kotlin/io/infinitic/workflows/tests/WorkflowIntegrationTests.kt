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

package io.infinitic.workflows.tests

import io.infinitic.client.Client
import io.infinitic.client.deferred.Deferred
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.storage.keySet.LoggedKeySetStorage
import io.infinitic.common.storage.keyValue.LoggedKeyValueStorage
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.exceptions.clients.CanceledWorkflow
import io.infinitic.metrics.global.engine.MetricsGlobalEngine
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.MetricsPerNameEngine
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.workflows.WorkflowTagEngine
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.registerTask
import io.infinitic.tasks.registerWorkflow
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tests.tasks.TaskA
import io.infinitic.workflows.tests.tasks.TaskAImpl
import io.infinitic.workflows.tests.workflows.Obj
import io.infinitic.workflows.tests.workflows.Obj1
import io.infinitic.workflows.tests.workflows.Obj2
import io.infinitic.workflows.tests.workflows.WorkflowA
import io.infinitic.workflows.tests.workflows.WorkflowAImpl
import io.infinitic.workflows.tests.workflows.WorkflowB
import io.infinitic.workflows.tests.workflows.WorkflowBImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.Instant
import java.util.UUID

val keyValueStorage = LoggedKeyValueStorage(InMemoryKeyValueStorage())
val keySetStorage = LoggedKeySetStorage(InMemoryKeySetStorage())
private val workflowTagStorage = BinaryWorkflowTagStorage(keyValueStorage, keySetStorage)
private val taskStateStorage = BinaryTaskStateStorage(keyValueStorage)
private val workflowStateStorage = BinaryWorkflowStateStorage(keyValueStorage)
private val metricsPerNameStateStorage = BinaryMetricsPerNameStateStorage(keyValueStorage)
private val metricsGlobalStateStorage = BinaryMetricsGlobalStateStorage(keyValueStorage)

private lateinit var taskEngine: TaskEngine
private lateinit var workflowTagEngine: WorkflowTagEngine
private lateinit var workflowEngine: WorkflowEngine
private lateinit var metricsPerNameEngine: MetricsPerNameEngine
private lateinit var metricsGlobalEngine: MetricsGlobalEngine
private lateinit var executor: TaskExecutor
private lateinit var client: Client
private lateinit var workflowA: WorkflowA
private lateinit var workflowB: WorkflowB
private lateinit var workflowATagged: WorkflowA

class WorkflowIntegrationTests : StringSpec({

    "empty Workflow" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { empty() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "void"
    }

    "get id from context" {
        var result: UUID
        var deferred: Deferred<UUID>
        // run system
        coroutineScope {
            init()
            deferred = client.async(workflowA) { context1() }
            result = deferred.await()
        }
        result shouldBe deferred.id
    }

    "get tags from context" {
        var result: Set<String>
        var deferred: Deferred<Set<String>>
        // run system
        coroutineScope {
            init()
            deferred = client.async(workflowATagged) { context2() }
            result = deferred.await()
        }
        result shouldBe setOf("foo", "bar")
    }

    "get meta from context" {
        var result: WorkflowMeta
        // run system
        coroutineScope {
            init()
            val workflowAMeta = client.newWorkflow(WorkflowA::class.java, meta = mapOf("foo" to "bar".toByteArray()))
            val deferred = client.async(workflowAMeta) { context3() }
            result = deferred.await()
        }
        result shouldBe WorkflowMeta(mapOf("foo" to "bar".toByteArray()))
    }

    "get workflow id from task context" {
        var result: UUID?
        var deferred: Deferred<UUID?>
        // run system
        coroutineScope {
            init()
            deferred = client.async(workflowA) { context4() }
            result = deferred.await()
        }
        result shouldBe deferred.id
    }

    "get workflow name from task context" {
        var result: String?
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { context5() }
            result = deferred.await()
        }
        result shouldBe WorkflowA::class.java.name
    }

    "Simple Sequential Workflow" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { seq1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "123"
    }

    "Wait for synchronous Workflow" {
        var result: String
        // run system
        coroutineScope {
            init()
            result = workflowA.seq1()
        }
        // checks number of task processing
        result shouldBe "123"
    }

    "Wait for asynchronous Workflow" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { seq1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { seq2() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "23ba"
    }

    "Sequential Workflow with an async branch" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { seq3() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { seq4() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "23bac"
    }

    "Test Deferred methods" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { deferred1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "truefalsefalsetrue"
    }

    "Or step with 3 async tasks" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { or1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBeIn listOf("ba", "dc", "fe")
    }

    "Combined And/Or step with 3 async tasks" {
        var result: Any
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { or2() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBeIn listOf(listOf("ba", "dc"), "fe")
    }

    "Or step with 3 async tasks through list" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { or3() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBeIn listOf("ba", "dc", "fe")
    }

    "Or step with Status checking" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { or4() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "baba"
    }

    "And step with 3 async tasks" {
        var result: List<String>
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { and1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through list" {
        var result: List<String>
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { and2() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through large list" {
        var result: List<String>
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { and3() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe MutableList(1_00) { "ba" }
    }

    "Inline task" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { inline1() }.id
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
    }

    "Inline task with asynchronous task inside" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { inline2() }.id
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
    }

    "Inline task with synchronous task inside" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { inline3() }.id
        }
        // check that the w is not terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldNotBe null
    }

    "Sequential Child Workflow" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { child1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "-abc-"
    }

    "Asynchronous Child Workflow" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { child2() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "21abc21"
    }

    "Nested Child Workflow" {
        var result: Long
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowB) { factorial(14) }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe 87178291200
    }

    "Check prop1" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop1() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "ac"
    }

    "Check prop2" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop2() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "acbd"
    }

    "Check prop3" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop3() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "acbd"
    }

    "Check prop4" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop4() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "acd"
    }

    "Check prop5" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop5() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "adbc"
    }

    "Check prop6" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop6() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "abab"
    }

    "Check prop7" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { prop7() }
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "acbd"
    }

    "Check prop6 sync" {
        var result: String
        // run system
        coroutineScope {
            init()
            result = workflowA.prop6()
        }
        result shouldBe "abab"
    }

    "Check multiple sync" {
        // run system
        var result1: String
        var result2: String
        coroutineScope {
            init()
            result1 = workflowA.seq1()
            result2 = client.newWorkflow<WorkflowA>().prop1()
        }
        result1 shouldBe "123"
        result2 shouldBe "ac"
    }

    "Waiting for event, sent after dispatched" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel1() }
            workflowA.channelA.send("test")
            result = deferred.await()
        }
        // check output
        result shouldBe "test"
    }

    "Waiting for event, sent by id" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel1() }
            client.getWorkflow<WorkflowA>(deferred.id).channelA.send("test")
            result = deferred.await()
        }
        // check output
        result shouldBe "test"
    }

    "Waiting for event, sent by tag" {
        var result: String
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowATagged) { channel1() }
            client.getWorkflow<WorkflowA>("foo").channelA.send("test")
            result = deferred.await()
        }
        // check output
        result shouldBe "test"
    }

    "Waiting for event, sent to the right channel" {
        var result: Any
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel2() }
            client.getWorkflow<WorkflowA>(deferred.id).channelA.send("test")
            result = deferred.await()
        }
        // check output
        result shouldBe "test"
    }

    "Waiting for event but sent to the wrong channel" {
        var result: Any
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel2() }
            client.getWorkflow<WorkflowA>(deferred.id).channelB.send("test")
            result = deferred.await()
        }
        // check output
        result::class.java.name shouldBe Instant::class.java.name
    }

    "Sending event before waiting for it prevents catching" {
        var result: Any
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel3() }
            client.getWorkflow<WorkflowA>(deferred.id).channelA.send("test")
            result = deferred.await()
        }
        // check output
        result::class.java.name shouldBe Instant::class.java.name
    }

    "Waiting for Obj event" {
        var result: Obj
        val obj1 = Obj1("foo", 42)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel4() }
            workflowA.channelObj.send(obj1)
            result = deferred.await()
        }
        // check output
        result shouldBe obj1
    }

    "Waiting for filtered event using jsonPath only" {
        var result: Obj
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel4bis() }
            workflowA.channelObj.send(obj1a)
            delay(50)
            workflowA.channelObj.send(obj1b)
            result = deferred.await()
        }
        // check output
        result shouldBe obj1b
    }

    "Waiting for filtered event using using jsonPath and criteria" {
        var result: Obj
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel4ter() }
            workflowA.channelObj.send(obj1a)
            delay(50)
            workflowA.channelObj.send(obj1b)
            result = deferred.await()
        }
        // check output
        result shouldBe obj1b
    }

    "Waiting for event of specific type" {
        var result: Obj
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel5() }
            workflowA.channelObj.send(obj2)
            delay(50)
            workflowA.channelObj.send(obj1)
            result = deferred.await()
        }
        // check output
        result shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath only" {
        var result: Obj
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel5bis() }
            workflowA.channelObj.send(obj3)
            delay(50)
            workflowA.channelObj.send(obj2)
            delay(50)
            workflowA.channelObj.send(obj1)
            result = deferred.await()
        }
        // check output
        result shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath and criteria" {
        var result: Obj
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel5ter() }
            client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj3)
            delay(50)
            client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj2)
            delay(50)
            client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj1)
            result = deferred.await()
        }
        // check output
        result shouldBe obj1
    }

    "Waiting for 2 events of specific types presented in wrong order" {
        var result: String
        val obj1 = Obj1("foo", 6)
        val obj2 = Obj2("bar", 7)
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { channel6() }
            client.getWorkflow<WorkflowA>(deferred.id).channelObj.send(obj2)
            delay(50)
            workflowA.channelObj.send(obj1)
            result = deferred.await()
        }
        // check output
        result shouldBe "foobar42"
    }

    "Tag should be added and deleted after completion" {
        var result: String
        var id: UUID
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowATagged) { channel1() }
            id = deferred.id
            // checks id has been added to tag storage
            yield()
            workflowTagStorage.getWorkflowIds(WorkflowTag("foo"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe true
            workflowTagStorage.getWorkflowIds(WorkflowTag("bar"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe true
            workflowATagged.channelA.send("test")
            result = deferred.await()
        }
        // check output
        result shouldBe "test"
        // checks id has been removed from tag storage
        workflowTagStorage.getWorkflowIds(WorkflowTag("foo"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe false
        workflowTagStorage.getWorkflowIds(WorkflowTag("bar"), WorkflowName(WorkflowA::class.java.name)).contains(WorkflowId(id)) shouldBe false
    }

    "Canceling async workflow" {
        var deferred: Deferred<String>
        // run system
        coroutineScope {
            init()
            deferred = client.async(workflowA) { channel1() }
            client.cancel(workflowA)
        }
        // check output
        workflowStateStorage.getState(WorkflowId(deferred.id)) shouldBe null
    }

    "Canceling sync workflow" {
        // run system
        coroutineScope {
            init()
            launch { client.cancel(workflowA) }
            shouldThrow<CanceledWorkflow> { workflowA.channel1() }
        }
    }

    "Canceling sync workflow with deferred" {
        var deferred: Deferred<String>
        // run system
        coroutineScope {
            init()
            deferred = client.async(workflowA) { channel1() }
            launch { client.cancel(workflowA) }
            shouldThrow<CanceledWorkflow> { deferred.await() }
        }
        // check output
        workflowStateStorage.getState(WorkflowId(deferred.id)) shouldBe null
    }
})

fun CoroutineScope.sendToClientResponse(msg: ClientMessage) = launch {
    client.handle(msg)
}

fun CoroutineScope.sendToWorkflowEngine(msg: WorkflowEngineMessage) = launch {
    workflowEngine.handle(msg)
}

fun CoroutineScope.sendToWorkflowEngineAfter(msg: WorkflowEngineMessage, after: MillisDuration) = launch {
    if (after.long > 0) { delay(after.long) }
    workflowEngine.handle(msg)
}

fun CoroutineScope.sendToWorkflowTagEngine(msg: WorkflowTagEngineMessage) = launch {
    workflowTagEngine.handle(msg)
}

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage) = launch {
    taskEngine.handle(msg)
}

fun CoroutineScope.sendToTaskEngineAfter(msg: TaskEngineMessage, after: MillisDuration) = launch {
    if (after.long > 0) { delay(after.long) }
    taskEngine.handle(msg)
}

fun CoroutineScope.sendToMetricsPerName(msg: MetricsPerNameMessage) = launch {
    metricsPerNameEngine.handle(msg)
}

fun CoroutineScope.sendToMetricsGlobal(msg: MetricsGlobalMessage) = launch {
    metricsGlobalEngine.handle(msg)
}

// without Dispatchers.IO we have some obscure race conditions when waiting in tasks
fun CoroutineScope.sendToWorkers(msg: TaskExecutorMessage) = launch(Dispatchers.IO) {
    executor.handle(msg)
}

fun CoroutineScope.init() {
    keyValueStorage.flush()
    keySetStorage.flush()

    val scope = this

    class ClientTest : Client() {
        override val clientName = ClientName("clientTest")
        override val sendToTaskTagEngine: SendToTaskTagEngine = { }
        override val sendToTaskEngine: SendToTaskEngine = { scope.sendToTaskEngine(it) }
        override val sendToWorkflowTagEngine: SendToWorkflowTagEngine = { scope.sendToWorkflowTagEngine(it) }
        override val sendToWorkflowEngine: SendToWorkflowEngine = { scope.sendToWorkflowEngine(it) }
        override fun close() {}
    }

    client = ClientTest()

    workflowA = client.newWorkflow(WorkflowA::class.java)
    workflowATagged = client.newWorkflow(WorkflowA::class.java, setOf("foo", "bar"))

    workflowB = client.newWorkflow(WorkflowB::class.java)

    workflowTagEngine = WorkflowTagEngine(
        workflowTagStorage,
        { sendToWorkflowEngine(it) }
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        { sendToClientResponse(it) },
        { },
        { msg, after -> sendToTaskEngineAfter(msg, after) },
        { sendToWorkflowEngine(it) },
        { sendToWorkers(it) },
        { sendToMetricsPerName(it) }
    )

    workflowEngine = WorkflowEngine(
        workflowStateStorage,
        { sendToClientResponse(it) },
        { sendToWorkflowTagEngine(it) },
        { sendToTaskEngine(it) },
        { sendToWorkflowEngine(it) },
        { msg, after -> sendToWorkflowEngineAfter(msg, after) }
    )

    metricsPerNameEngine = MetricsPerNameEngine(
        metricsPerNameStateStorage,
        { sendToMetricsGlobal(it) }
    )

    metricsGlobalEngine = MetricsGlobalEngine(metricsGlobalStateStorage)

    executor = TaskExecutor(
        { sendToTaskEngine(it) },
        TaskExecutorRegisterImpl()
    )
    executor.registerTask<TaskA> { TaskAImpl() }
    executor.registerWorkflow<WorkflowA> { WorkflowAImpl() }
    executor.registerWorkflow<WorkflowB> { WorkflowBImpl() }
}
