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

//import io.infinitic.avro.taskManager.data.AvroTaskStatus
//import io.infinitic.tests.workflows.inMemory.InMemoryDispatcherTest
//import io.infinitic.tests.workflows.inMemory.InMemoryStorageTest
//import io.infinitic.common.workflows.data.workflows.WorkflowInstance
//import io.infinitic.tests.workflows.samples.TaskA
//import io.infinitic.tests.workflows.samples.TaskAImpl
//import io.infinitic.tests.workflows.samples.WorkflowA
//import io.infinitic.tests.workflows.samples.WorkflowAImpl
//import io.infinitic.tests.workflows.samples.WorkflowB
//import io.infinitic.tests.workflows.samples.WorkflowBImpl
//import io.kotest.core.spec.style.StringSpec
//import io.kotest.matchers.collections.shouldBeIn
//import io.kotest.matchers.shouldBe
//import io.mockk.mockk
//import kotlinx.coroutines.coroutineScope
//import org.slf4j.Logger
//
//private val mockLogger = mockk<Logger>(relaxed = true)
//
//private val storage = InMemoryStorageTest()
//private val dispatcher = InMemoryDispatcherTest(storage)
//private val client = dispatcher.client
//private val worker = dispatcher.worker
//
//private lateinit var status: AvroTaskStatus
//
//class WorkflowIntegrationTests : StringSpec({
//    worker.register<TaskA> { TaskAImpl() }
//    worker.register<WorkflowA> { WorkflowAImpl() }
//    worker.register<WorkflowB> { WorkflowBImpl() }
//
//    var workflowInstance: WorkflowInstance
//
//    beforeTest {
//        storage.reset()
//        dispatcher.reset()
//    }
//
//    "empty Workflow" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { empty() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "void"
//    }
//
//    "Simple Sequential Workflow" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { seq1() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "123"
//    }
//
//    "Sequential Workflow with an async task" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { seq2() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "23ba"
//    }
//
//    "Sequential Workflow with an async branch" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { seq3() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "23ba"
//    }
//
//    "Sequential Workflow with an async branch with 2 tasks" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { seq4() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "23bac"
//    }
//
//    "Or step with 3 async tasks" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { or1() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBeIn listOf("ba", "dc", "fe")
//    }
//
//    "Combined And/Or step with 3 async tasks" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { or2() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBeIn listOf(listOf("ba", "dc"), "fe")
//    }
//
//    "Or step with 3 async tasks through list" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { or3() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBeIn listOf("ba", "dc", "fe")
//    }
//
//    "And step with 3 async tasks" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { and1() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe listOf("ba", "dc", "fe")
//    }
//
//    "And step with 3 async tasks through list" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { and2() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe listOf("ba", "dc", "fe")
//    }
//
//    "And step with 3 async tasks through large list" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { and3() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe MutableList(1_000) { "ba" }
//    }
//
//    "Inline task" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { inline() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//    }
//
//    "Inline task with asynchronous task inside" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { inline2() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//    }
//
//    "Inline task with synchronous task inside" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { inline3() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe false
//    }
//
//    "Sequential Child Workflow" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { child1() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "-abc-"
//    }
//
//    "Asynchronous Child Workflow" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowA::class.java) { child2() }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe "21abc21"
//    }
//
//    "Nested Child Workflow" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatch(WorkflowB::class.java) { factorial(14) }
//        }
//        // check that the w is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        dispatcher.workflowOutput shouldBe 87178291200
//    }
//})
