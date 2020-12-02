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
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.data.workflows.WorkflowInstance
import io.infinitic.common.workflows.engine.messages.WorkflowCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.TaskStateKeyValueStorage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.WorkflowStateKeyValueStorage
import io.infinitic.workflows.tests.samples.TaskA
import io.infinitic.workflows.tests.samples.TaskAImpl
import io.infinitic.workflows.tests.samples.WorkflowA
import io.infinitic.workflows.tests.samples.WorkflowAImpl
import io.infinitic.workflows.tests.samples.WorkflowB
import io.infinitic.workflows.tests.samples.WorkflowBImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var workflowOutput: Any? = null
private val workflowStateStorage = WorkflowStateKeyValueStorage(InMemoryStorage())
private val taskStateStorage = TaskStateKeyValueStorage(InMemoryStorage())
private val monitoringPerNameStateStorage = MonitoringPerNameStateKeyValueStorage(InMemoryStorage())
private val monitoringGlobalStateStorage = MonitoringGlobalStateKeyValueStorage(InMemoryStorage())

private lateinit var workflowEngine: WorkflowEngine
private lateinit var taskEngine: TaskEngine
private lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
private lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
private lateinit var executor: TaskExecutor
private lateinit var client: Client

fun CoroutineScope.sendToWorkflowEngine(msg: WorkflowEngineMessage, after: Float) {
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

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage, after: Float) {
    launch {
        if (after > 0F) {
            delay((1000 * after).toLong())
        }
        taskEngine.handle(msg)
    }
}

fun CoroutineScope.sendToMonitoringPerName(msg: MonitoringPerNameEngineMessage) {
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
    launch {
        executor.handle(msg)
    }
}

fun CoroutineScope.init() {
    workflowStateStorage.flush()
    taskStateStorage.flush()
    monitoringPerNameStateStorage.flush()
    monitoringGlobalStateStorage.flush()
    workflowOutput = null

    client = Client(
        { msg: TaskEngineMessage -> sendToTaskEngine(msg, 0F) },
        { msg: WorkflowEngineMessage -> sendToWorkflowEngine(msg, 0F) }
    )

    workflowEngine = WorkflowEngine(
        workflowStateStorage,
        { msg: WorkflowEngineMessage, after: Float -> sendToWorkflowEngine(msg, after) },
        { msg: TaskEngineMessage, after: Float -> sendToTaskEngine(msg, after) }
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        { _: TaskEngineMessage -> Unit },
        { msg: TaskEngineMessage, after: Float -> sendToTaskEngine(msg, after) },
        { msg: MonitoringPerNameEngineMessage -> sendToMonitoringPerName(msg) },
        { msg: TaskExecutorMessage -> sendToWorkers(msg) },
        { msg: WorkflowEngineMessage, after: Float -> sendToWorkflowEngine(msg, after) }
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(monitoringPerNameStateStorage) {
        msg: MonitoringGlobalMessage ->
        sendToMonitoringGlobal(msg)
    }

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    executor = TaskExecutor { msg: TaskEngineMessage, after: Float -> sendToTaskEngine(msg, after) }
    executor.register<TaskA> { TaskAImpl() }
    executor.register<WorkflowA> { WorkflowAImpl() }
    executor.register<WorkflowB> { WorkflowBImpl() }
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
