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
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.Name
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.storage.keySet.LoggedKeySetStorage
import io.infinitic.common.storage.keyValue.LoggedKeyValueStorage
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.WorkflowCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.BinaryMonitoringGlobalStateStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.BinaryMonitoringPerNameStateStorage
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.storage.BinaryTagStateStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.register
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tests.tasks.TaskA
import io.infinitic.workflows.tests.tasks.TaskAImpl
import io.infinitic.workflows.tests.workflows.Obj1
import io.infinitic.workflows.tests.workflows.Obj2
import io.infinitic.workflows.tests.workflows.WorkflowA
import io.infinitic.workflows.tests.workflows.WorkflowAImpl
import io.infinitic.workflows.tests.workflows.WorkflowB
import io.infinitic.workflows.tests.workflows.WorkflowBImpl
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

private var workflowOutput: Any? = null
val keyValueStorage = LoggedKeyValueStorage(InMemoryKeyValueStorage())
val keySetStorage = LoggedKeySetStorage(InMemoryKeySetStorage())
private val tagStateStorage = BinaryTagStateStorage(keyValueStorage, keySetStorage)
private val taskStateStorage = BinaryTaskStateStorage(keyValueStorage)
private val workflowStateStorage = BinaryWorkflowStateStorage(keyValueStorage)
private val monitoringPerNameStateStorage = BinaryMonitoringPerNameStateStorage(keyValueStorage)
private val monitoringGlobalStateStorage = BinaryMonitoringGlobalStateStorage(keyValueStorage)

private lateinit var tagEngine: TagEngine
private lateinit var taskEngine: TaskEngine
private lateinit var workflowEngine: WorkflowEngine
private lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
private lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
private lateinit var executor: TaskExecutor
private lateinit var client: Client
private lateinit var workflowA: WorkflowA
private lateinit var workflowB: WorkflowB
private lateinit var workflowATagged: WorkflowA

class WorkflowIntegrationTests : StringSpec({
    var workflowId: WorkflowId

    "empty Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { empty() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "void"
    }

    "Simple Sequential Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { seq1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "123"
    }

    "Wait for synchronous Workflow" {
        var result: String? = null
        // run system
        coroutineScope {
            init()
            result = workflowA.seq1()
        }
        // checks number of task processing
        result shouldBe "123"
    }

    "Wait for asynchronous Workflow" {
        var result: String? = null
        // run system
        coroutineScope {
            init()
            val deferred = client.async(workflowA) { seq1() }
            workflowId = WorkflowId(deferred.id)
            result = deferred.await()
        }
        // checks number of task processing
        result shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { seq2() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "23ba"
    }

    "Sequential Workflow with an async branch" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { seq3() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { seq4() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "23bac"
    }

    "Test Deferred methods" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { deferred1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "truefalsefalsetrue"
    }

    "Or step with 3 async tasks" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { or1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBeIn listOf("ba", "dc", "fe")
    }

    "Combined And/Or step with 3 async tasks" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { or2() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBeIn listOf(listOf("ba", "dc"), "fe")
    }

    "Or step with 3 async tasks through list" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { or3() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBeIn listOf("ba", "dc", "fe")
    }

    "Or step with Status checking" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { or4() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "baba"
    }

    "And step with 3 async tasks" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { and1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through list" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { and2() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through large list" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { and3() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe MutableList(1_00) { "ba" }
    }

    "Inline task" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { inline1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
    }

    "Inline task with asynchronous task inside" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { inline2() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
    }

    "Inline task with synchronous task inside" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { inline3() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldNotBe null
    }

    "Sequential Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { child1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "-abc-"
    }

    "Asynchronous Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { child2() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "21abc21"
    }

    "Nested Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowB) { factorial(14) }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe 87178291200
    }

    "Check prop1" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop1() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "ac"
    }

    "Check prop2" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop2() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acbd"
    }

    "Check prop3" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop3() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acbd"
    }

    "Check prop4" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop4() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acd"
    }

    "Check prop5" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop5() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "adbc"
    }

    "Check prop6" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop6() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "abab"
    }

    "Check prop7" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { prop7() }.id)
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acbd"
    }

    "Check prop6 sync" {
        // run system
        var result: String
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
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel1() }.id
            workflowA.channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "test"
    }

    "Waiting for event, sent by id" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel1() }.id
            client.getWorkflow<WorkflowA>(id).channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "test"
    }

    "Waiting for event, sent by tag" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowATagged) { channel1() }.id
            client.getWorkflow<WorkflowA>("foo").channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "test"
    }

    "Waiting for event, sent to the right channel" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel2() }.id
            client.getWorkflow<WorkflowA>(id).channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "test"
    }

    "Waiting for event but sent to the wrong channel" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel2() }.id
            client.getWorkflow<WorkflowA>(id).channelB.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput!!::class.java.name shouldBe Instant::class.java.name
    }

    "Sending event before waiting for it prevents catching" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel3() }.id
            client.getWorkflow<WorkflowA>(id).channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput!!::class.java.name shouldBe Instant::class.java.name
    }

    "Waiting for Obj event" {
        var id: UUID
        val obj1 = Obj1("foo", 42)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel4() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe obj1
    }

    "Waiting for filtered event using jsonPath only" {
        var id: UUID
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel4bis() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1a)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1b)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe obj1b
    }

    "Waiting for filtered event using using jsonPath and criteria" {
        var id: UUID
        val obj1a = Obj1("oof", 12)
        val obj1b = Obj1("foo", 12)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel4ter() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1a)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1b)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe obj1b
    }

    "Waiting for event of specific type" {
        var id: UUID
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel5() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj2)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath only" {
        var id: UUID
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel5bis() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj3)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj2)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe obj1
    }

    "Waiting event of specific type filtered using jsonPath and criteria" {
        var id: UUID
        val obj1 = Obj1("foo", 42)
        val obj2 = Obj2("foo", 42)
        val obj3 = Obj1("oof", 42)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel5ter() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj3)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj2)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe obj1
    }

    "Waiting for 2 events of specific types presented in wrong order" {
        var id: UUID
        val obj1 = Obj1("foo", 6)
        val obj2 = Obj2("bar", 7)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel6() }.id
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj2)
            delay(50)
            client.getWorkflow<WorkflowA>(id).channelObj.send(obj1)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "foobar42"
    }

    "Tag should be added and deleted after completion" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowATagged) { channel1() }.id
            // checks id has been added to tag storage
            yield()
            tagStateStorage.getIds(Tag("foo"), Name(WorkflowA::class.java.name)).contains(id) shouldBe true
            tagStateStorage.getIds(Tag("bar"), Name(WorkflowA::class.java.name)).contains(id) shouldBe true
            workflowATagged.channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "test"
        // checks id has been removed from tag storage
        tagStateStorage.getIds(Tag("foo"), Name(WorkflowA::class.java.name)).contains(id) shouldBe false
        tagStateStorage.getIds(Tag("bar"), Name(WorkflowA::class.java.name)).contains(id) shouldBe false
    }
})

fun CoroutineScope.sendToClientResponse(msg: ClientMessage) = launch {
    client.handle(msg)
}

fun CoroutineScope.sendToWorkflowEngine(msg: WorkflowEngineMessage, after: MillisDuration) = launch {
    if (after.long > 0) { delay(after.long) }
    workflowEngine.handle(msg)

    // defines output if reached
    if (msg is WorkflowCompleted) {
        workflowOutput = msg.workflowReturnValue.get()
    }
}

fun CoroutineScope.sendToTagEngine(msg: TagEngineMessage) = launch {
    tagEngine.handle(msg)
}

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage, after: MillisDuration) = launch {
    if (after.long > 0) { delay(after.long) }
    taskEngine.handle(msg)
}

fun CoroutineScope.sendToMonitoringPerName(msg: MetricsPerNameMessage) = launch {
    monitoringPerNameEngine.handle(msg)
}

fun CoroutineScope.sendToMonitoringGlobal(msg: MetricsGlobalMessage) = launch {
    monitoringGlobalEngine.handle(msg)
}

// without Dispatchers.IO we have some obscure race conditions when waiting in tasks
fun CoroutineScope.sendToWorkers(msg: TaskExecutorMessage) = launch(Dispatchers.IO) {
    executor.handle(msg)
}

fun CoroutineScope.init() {
    keyValueStorage.flush()
    keySetStorage.flush()

    workflowOutput = null

    client = Client.with(
        ClientName("client: testing"),
        { sendToTagEngine(it) },
        { msg, after -> sendToTaskEngine(msg, after) },
        { msg, after -> sendToWorkflowEngine(msg, after) }
    )

    workflowA = client.newWorkflow(WorkflowA::class.java)
    workflowATagged = client.newWorkflow(WorkflowA::class.java, setOf("foo", "bar"))
    workflowB = client.newWorkflow(WorkflowB::class.java)

    tagEngine = TagEngine(
        tagStateStorage,
        { sendToClientResponse(it) },
        { msg, after -> sendToTaskEngine(msg, after) },
        { msg, after -> sendToWorkflowEngine(msg, after) }
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        { sendToClientResponse(it) },
        { sendToTagEngine(it) },
        { msg, after -> sendToTaskEngine(msg, after) },
        { msg, after -> sendToWorkflowEngine(msg, after) },
        { sendToWorkers(it) },
        { sendToMonitoringPerName(it) }
    )

    workflowEngine = WorkflowEngine(
        workflowStateStorage,
        { sendToClientResponse(it) },
        { sendToTagEngine(it) },
        { msg, after -> sendToTaskEngine(msg, after) },
        { msg, after -> sendToWorkflowEngine(msg, after) }
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(
        monitoringPerNameStateStorage,
        { sendToMonitoringGlobal(it) }
    )

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    executor = TaskExecutor(
        { msg, after -> sendToTaskEngine(msg, after) },
        TaskExecutorRegisterImpl()
    )
    executor.register<TaskA> { TaskAImpl() }
    executor.register<WorkflowA> { WorkflowAImpl() }
    executor.register<WorkflowB> { WorkflowBImpl() }
}
