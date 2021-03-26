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

import io.infinitic.cache.no.NoCache
import io.infinitic.client.Client
import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.transport.SendToClientResponse
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameMessage
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.WorkflowCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.exceptions.SendToChannelFailed
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameOutput
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.storage.TagStateCachedKeyStorage
import io.infinitic.tags.engine.transport.TagEngineOutput
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateCachedKeyStorage
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.executor.transport.TaskExecutorOutput
import io.infinitic.tasks.register
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.events.NoWorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateCachedKeyStorage
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput
import io.infinitic.workflows.tests.tasks.TaskA
import io.infinitic.workflows.tests.tasks.TaskAImpl
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
import java.time.Instant
import java.util.UUID

private var workflowOutput: Any? = null
private val tagStateStorage = TagStateCachedKeyStorage(
    InMemoryKeyValueStorage(),
    NoCache(),
    InMemoryKeySetStorage(),
    NoCache()
)
private val taskStateStorage = TaskStateCachedKeyStorage(InMemoryKeyValueStorage(), NoCache())
private val workflowStateStorage = WorkflowStateCachedKeyStorage(InMemoryKeyValueStorage(), NoCache())
private val monitoringPerNameStateStorage = MonitoringPerNameStateKeyValueStorage(InMemoryKeyValueStorage(), NoCache())
private val monitoringGlobalStateStorage = MonitoringGlobalStateKeyValueStorage(InMemoryKeyValueStorage(), NoCache())

private lateinit var tagEngine: TagEngine
private lateinit var taskEngine: TaskEngine
private lateinit var workflowEngine: WorkflowEngine
private lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
private lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
private lateinit var executor: TaskExecutor
private lateinit var client: Client
private lateinit var workflowA: WorkflowA
private lateinit var workflowB: WorkflowB

class WorkflowIntegrationTests : StringSpec({
    var workflowId: WorkflowId

    "empty Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { empty() })
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
            workflowId = WorkflowId(client.async(workflowA) { seq1() })
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { seq2() })
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
            workflowId = WorkflowId(client.async(workflowA) { seq3() })
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
            workflowId = WorkflowId(client.async(workflowA) { seq4() })
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
            workflowId = WorkflowId(client.async(workflowA) { deferred1() })
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
            workflowId = WorkflowId(client.async(workflowA) { or1() })
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
            workflowId = WorkflowId(client.async(workflowA) { or2() })
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
            workflowId = WorkflowId(client.async(workflowA) { or3() })
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
            workflowId = WorkflowId(client.async(workflowA) { or4() })
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
            workflowId = WorkflowId(client.async(workflowA) { and1() })
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
            workflowId = WorkflowId(client.async(workflowA) { and2() })
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
            workflowId = WorkflowId(client.async(workflowA) { and3() })
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
            workflowId = WorkflowId(client.async(workflowA) { inline1() })
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
    }

    "Inline task with asynchronous task inside" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { inline2() })
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldBe null
    }

    "Inline task with synchronous task inside" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { inline3() })
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowId) shouldNotBe null
    }

    "Sequential Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowId = WorkflowId(client.async(workflowA) { child1() })
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
            workflowId = WorkflowId(client.async(workflowA) { child2() })
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
            workflowId = WorkflowId(client.async(workflowB) { factorial(14) })
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
            workflowId = WorkflowId(client.async(workflowA) { prop1() })
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
            workflowId = WorkflowId(client.async(workflowA) { prop2() })
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
            workflowId = WorkflowId(client.async(workflowA) { prop3() })
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
            workflowId = WorkflowId(client.async(workflowA) { prop4() })
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
            workflowId = WorkflowId(client.async(workflowA) { prop5() })
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
            workflowId = WorkflowId(client.async(workflowA) { prop6() })
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
            workflowId = WorkflowId(client.async(workflowA) { prop7() })
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
            result2 = workflowA.prop1()
        }
        result1 shouldBe "123"
        result2 shouldBe "ac"
    }

    "Waiting for event" {
        var id: UUID
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel1() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelA.send("test")
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
            id = client.async(workflowA) { channel2() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelA.send("test")
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
            id = client.async(workflowA) { channel2() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelB.send("test")
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
            id = client.async(workflowA) { channel3() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelA.send("test")
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput!!::class.java.name shouldBe Instant::class.java.name
    }

    "Sending event synchronously to unknown workflow should throw exception" {
        // run system
        coroutineScope {
            init()
            val id = client.async(workflowA) { channel3() }
            shouldThrow<SendToChannelFailed> {
                client.workflow<WorkflowA>("other$id").channelA.send("test")
            }
        }
    }

//    "Sending event synchronously to completed workflow should throw exception" {
//        // run system
//        coroutineScope {
//            init()
//            val id = client.async(workflowA) { channel3() }
//            shouldThrow<SendToChannelFailed> {
//                client.workflow<WorkflowA>("other$id").channelA.send("test")
//            }
//        }
//    }

    "Waiting for Obj event" {
        var id: UUID
        val obj1 = Obj1("foo", 42)
        // run system
        coroutineScope {
            init()
            id = client.async(workflowA) { channel4() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1)
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
            id = client.async(workflowA) { channel4bis() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1a)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1b)
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
            id = client.async(workflowA) { channel4ter() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1a)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1b)
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
            id = client.async(workflowA) { channel5() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj2)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1)
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
            id = client.async(workflowA) { channel5bis() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj3)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj2)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1)
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
            id = client.async(workflowA) { channel5ter() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj3)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj2)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1)
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
            id = client.async(workflowA) { channel6() }
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj2)
            delay(50)
            client.workflow<WorkflowA>("${Tag.of(id)}").channelObj.send(obj1)
        }
        // check that the w is terminated
        workflowStateStorage.getState(WorkflowId(id)) shouldBe null
        // check output
        workflowOutput shouldBe "foobar42"
    }
})

class InMemoryWorkflowEngineOutput(private val scope: CoroutineScope) : WorkflowEngineOutput {
    override val sendToClientResponseFn: SendToClientResponse =
        { msg: ClientResponseMessage -> scope.sendToClientResponse(msg) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine =
        { msg: WorkflowEngineMessage, after: MillisDuration -> scope.sendToWorkflowEngine(msg, after) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }
}

class InMemoryTagEngineOutput(private val scope: CoroutineScope) : TagEngineOutput {
    override val sendToClientResponseFn: SendToClientResponse =
        { msg: ClientResponseMessage -> scope.sendToClientResponse(msg) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine =
        { msg: WorkflowEngineMessage, after: MillisDuration -> scope.sendToWorkflowEngine(msg, after) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }
}

class InMemoryTaskEngineOutput(private val scope: CoroutineScope) : TaskEngineOutput {
    override val sendToClientResponseFn: SendToClientResponse =
        { msg: ClientResponseMessage -> scope.sendToClientResponse(msg) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine =
        { msg: WorkflowEngineMessage, after: MillisDuration -> scope.sendToWorkflowEngine(msg, after) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }

    override val sendToTaskExecutorsFn: SendToTaskExecutors =
        { msg: TaskExecutorMessage -> scope.sendToWorkers(msg) }

    override val sendToMonitoringPerNameFn: SendToMonitoringPerName =
        { msg: MonitoringPerNameMessage -> scope.sendToMonitoringPerName(msg) }
}

class InMemoryMonitoringPerNameOutput(private val scope: CoroutineScope) : MonitoringPerNameOutput {

    override val sendToMonitoringGlobalFn: SendToMonitoringGlobal =
        { msg: MonitoringGlobalMessage -> scope.sendToMonitoringGlobal(msg) }
}

class InMemoryTaskExecutorOutput(private val scope: CoroutineScope) : TaskExecutorOutput {

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }
}

class TestClientOutput(private val scope: CoroutineScope) : ClientOutput {
    override val clientName = ClientName("client: testing")

    override val sendToTagEngineFn: SendToTagEngine =
        { msg: TagEngineMessage -> scope.sendToTagEngine(msg) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine =
        { msg: WorkflowEngineMessage, after: MillisDuration -> scope.sendToWorkflowEngine(msg, after) }
}

fun CoroutineScope.sendToClientResponse(msg: ClientResponseMessage) {
    launch {
        client.handle(msg)
    }
}

fun CoroutineScope.sendToWorkflowEngine(msg: WorkflowEngineMessage, after: MillisDuration) {
    launch {
        if (after.long > 0) { delay(after.long) }
        workflowEngine.handle(msg)

        // defines output if reached
        if (msg is WorkflowCompleted) {
            workflowOutput = msg.workflowReturnValue.get()
        }
    }
}

fun CoroutineScope.sendToTagEngine(msg: TagEngineMessage) {
    launch {
        tagEngine.handle(msg)
    }
}

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage, after: MillisDuration) {
    launch {
        if (after.long > 0) { delay(after.long) }
        taskEngine.handle(msg)
    }
}

fun CoroutineScope.sendToMonitoringPerName(msg: MonitoringPerNameMessage) {
    launch {
        monitoringPerNameEngine.handle(msg)
    }
}

fun CoroutineScope.sendToMonitoringGlobal(msg: MonitoringGlobalMessage) {
    launch {
        monitoringGlobalEngine.handle(msg)
    }
}

fun CoroutineScope.sendToWorkers(msg: TaskExecutorMessage) {
    // without Dispatchers.IO we have some obscure race conditions when waiting in tasks
    launch(Dispatchers.IO) {
        executor.handle(msg)
    }
}

fun CoroutineScope.init() {
    workflowStateStorage.flush()
    taskStateStorage.flush()
    monitoringPerNameStateStorage.flush()
    monitoringGlobalStateStorage.flush()
    workflowOutput = null

    client = Client(TestClientOutput(this))

    workflowA = client.workflow(WorkflowA::class.java)
    workflowB = client.workflow(WorkflowB::class.java)

    tagEngine = TagEngine(
        tagStateStorage,
        InMemoryTagEngineOutput(this)
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        NoTaskEventStorage(),
        InMemoryTaskEngineOutput(this)
    )

    workflowEngine = WorkflowEngine(
        workflowStateStorage,
        NoWorkflowEventStorage(),
        InMemoryWorkflowEngineOutput(this)
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(
        monitoringPerNameStateStorage,
        InMemoryMonitoringPerNameOutput(this)
    )

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    executor = TaskExecutor(InMemoryTaskExecutorOutput(this), TaskExecutorRegisterImpl())
    executor.register<TaskA> { TaskAImpl() }
    executor.register<WorkflowA> { WorkflowAImpl() }
    executor.register<WorkflowB> { WorkflowBImpl() }
}
