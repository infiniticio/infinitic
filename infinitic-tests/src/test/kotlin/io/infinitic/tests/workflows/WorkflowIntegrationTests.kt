// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.tests.workflows

import io.infinitic.client.Client
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalMessage
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineMessage
import io.infinitic.common.tasks.messages.workerMessages.WorkerMessage
import io.infinitic.common.workflows.data.workflows.WorkflowInstance
import io.infinitic.common.workflows.messages.WorkflowCompleted
import io.infinitic.common.workflows.messages.WorkflowEngineMessage
import io.infinitic.engines.monitoringGlobal.engine.MonitoringGlobalEngine
import io.infinitic.engines.monitoringGlobal.storage.MonitoringGlobalStateInMemoryStorage
import io.infinitic.engines.monitoringPerName.engine.MonitoringPerNameEngine
import io.infinitic.engines.monitoringPerName.storage.MonitoringPerNameStateInMemoryStorage
import io.infinitic.engines.tasks.engine.TaskEngine
import io.infinitic.engines.tasks.storage.TaskStateInMemoryStorage
import io.infinitic.engines.workflows.engine.WorkflowEngine
import io.infinitic.engines.workflows.engine.workflowHook
import io.infinitic.engines.workflows.storage.WorkflowStateInMemoryStorage
import io.infinitic.tests.workflows.samples.TaskA
import io.infinitic.tests.workflows.samples.TaskAImpl
import io.infinitic.tests.workflows.samples.WorkflowA
import io.infinitic.tests.workflows.samples.WorkflowAImpl
import io.infinitic.tests.workflows.samples.WorkflowB
import io.infinitic.tests.workflows.samples.WorkflowBImpl
import io.infinitic.worker.Worker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val workflowStateStorage = WorkflowStateInMemoryStorage()
private val taskStateStorage = TaskStateInMemoryStorage()
private val monitoringPerNameStateStorage = MonitoringPerNameStateInMemoryStorage()
private val monitoringGlobalStateStorage = MonitoringGlobalStateInMemoryStorage()
var workflowOutput: Any? = null

lateinit var workflowEngine: WorkflowEngine
lateinit var taskEngine: TaskEngine
lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
lateinit var worker: Worker
lateinit var client: Client

fun CoroutineScope.send(msg: WorkflowEngineMessage, after: Float) {
    launch {
        if (after > 0F) {
            delay((1000 * after).toLong())
        }
        workflowEngine.handle(msg)

        // defines output if reached
        if (msg is WorkflowCompleted) {
            workflowOutput = msg.workflowOutput.get()
        }
    }
}

fun CoroutineScope.send(msg: TaskEngineMessage, after: Float) {
    launch {
        if (after > 0F) {
            delay((1000 * after).toLong())
        }
        taskEngine.handle(msg)

        // workflow hook (need to send taskCompleted to workflow engine)
        taskEngine.workflowHook(msg)?.let { send(it, 0F) }
    }
}

fun CoroutineScope.send(msg: MonitoringPerNameEngineMessage) {
    launch {
        monitoringPerNameEngine.handle(msg)
    }
}

fun CoroutineScope.send(msg: MonitoringGlobalMessage) {
    launch {
        monitoringGlobalEngine.handle(msg)
    }
}

fun CoroutineScope.send(msg: WorkerMessage) {
    launch {
        worker.handle(msg)
    }
}

fun CoroutineScope.init() {
    workflowStateStorage.flush()
    taskStateStorage.flush()
    monitoringPerNameStateStorage.flush()
    monitoringGlobalStateStorage.flush()
    workflowOutput = null

    client = Client(
        { msg: TaskEngineMessage -> send(msg, 0F) },
        { msg: WorkflowEngineMessage -> send(msg, 0F) }
    )

    workflowEngine = WorkflowEngine(
        workflowStateStorage,
        { msg: WorkflowEngineMessage, after: Float -> send(msg, after) },
        { msg: TaskEngineMessage -> send(msg, 0F) }
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        { msg: TaskEngineMessage, after: Float -> send(msg, after) },
        { msg: MonitoringPerNameEngineMessage -> send(msg) },
        { msg: WorkerMessage -> send(msg) }
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(monitoringPerNameStateStorage) {
        msg: MonitoringGlobalMessage -> send(msg)
    }

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    worker = Worker { msg: TaskEngineMessage -> send(msg, 0F) }
    worker.register<TaskA> { TaskAImpl() }
    worker.register<WorkflowA> { WorkflowAImpl() }
    worker.register<WorkflowB> { WorkflowBImpl() }
}

class WorkflowIntegrationTests : StringSpec({
    var workflowInstance: WorkflowInstance

    "empty Workflow" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { empty() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "void"
    }

    "Simple Sequential Workflow" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { seq1() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { seq2() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "23ba"
    }

    "Sequential Workflow with an async branch" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { seq3() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { seq4() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "23bac"
    }

    "Or step with 3 async tasks" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { or1() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBeIn listOf("ba", "dc", "fe")
    }

    "Combined And/Or step with 3 async tasks" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { or2() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBeIn listOf(listOf("ba", "dc"), "fe")
    }

    "Or step with 3 async tasks through list" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { or3() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBeIn listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { and1() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through list" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { and2() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe listOf("ba", "dc", "fe")
    }

    "And step with 3 async tasks through large list" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { and3() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe MutableList(1_00) { "ba" }
    }

    "Inline task" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { inline() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
    }

    "Inline task with asynchronous task inside" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { inline2() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
    }

    "Inline task with synchronous task inside" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { inline3() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldNotBe null
    }

    "Sequential Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { child1() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "-abc-"
    }

    "Asynchronous Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { child2() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "21abc21"
    }

    "Nested Child Workflow" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowB::class.java) { factorial(14) }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe 87178291200
    }

    "Check prop1" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { prop1() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "ac"
    }

    "Check prop2" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { prop2() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acbd"
    }

    "Check prop3" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { prop3() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acbd"
    }

    "Check prop4" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { prop4() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "acd"
    }

    "Check prop5" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { prop5() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "adbc"
    }

    "Check prop6" {
        // run system
        coroutineScope {
            init()
            workflowInstance = client.dispatch(WorkflowA::class.java) { prop6() }
        }
        // check that the w is terminated
        workflowStateStorage.getState(workflowInstance.workflowId) shouldBe null
        // checks number of task processing
        workflowOutput shouldBe "abab"
    }
})
