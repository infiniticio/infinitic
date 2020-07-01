package com.zenaton.workflowManager.avroEngines

import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.avroEngines.AvroMonitoringGlobal
import com.zenaton.jobManager.avroEngines.AvroMonitoringPerName
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorker
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorkerDecision
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorkerTask
import com.zenaton.workflowManager.avroEngines.workflowInMemory.InMemoryDispatcher
import com.zenaton.workflowManager.avroEngines.workflowInMemory.InMemoryStorage
import com.zenaton.workflowManager.messages.AvroDispatchWorkflow
import com.zenaton.workflowManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.slf4j.Logger
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryDispatcher as SyncJobDispatcher
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryStorage as SyncJobStorage

private val logger = mockk<Logger>(relaxed = true)

private val taskEngine = AvroJobEngine()
private val taskMonitoringPerName = AvroMonitoringPerName()
private val taskMonitoringGlobal = AvroMonitoringGlobal()
private val worker = InMemoryWorkerTask()
private val taskDispatcher = SyncJobDispatcher(taskEngine, taskMonitoringPerName, taskMonitoringGlobal, worker)
private val taskStorage = SyncJobStorage()

private val decisionEngine = AvroJobEngine()
private val decisionMonitoringPerName = AvroMonitoringPerName()
private val decisionMonitoringGlobal = AvroMonitoringGlobal()
private val decider = InMemoryWorkerDecision()
private val decisionDispatcher = SyncJobDispatcher(decisionEngine, decisionMonitoringPerName, decisionMonitoringGlobal, decider)
private val decisionStorage = SyncJobStorage()

private val workflowEngine = AvroWorkflowEngine()
private val workflowDispatcher = InMemoryDispatcher(workflowEngine, decisionEngine, taskEngine)
private val workflowStorage = InMemoryStorage()

class AvroWorkflowEngineTests : StringSpec({
    beforeTest {
        worker.jobA = mockk()
        worker.jobB = mockk()
        worker.jobC = mockk()
        every { worker.jobA.handle() } just Runs
        every { worker.jobB.handle() } just Runs
        every { worker.jobC.handle() } just Runs
        taskStorage.init()
    }

    "Job succeeds at first try" {
        // all jobs will succeed
        worker.behavior = { InMemoryWorker.Status.SUCCESS }
        // run system
        val dispatch = getAvroDispatchWorkflow()
//        coroutineScope {
//            workflowDispatcher.scope = this
//            workflowDispatcher.toWorkflowEngine(dispatch)
//        }
//        // check that workflow     is completed
//        taskStorage.jobEngineStore[dispatch.workflowId] shouldBe null
//        // checks scenarios
//        verifyAll {
//            worker.jobA.handle()
//            worker.jobB.handle()
//        }
//        confirmVerified(worker.jobA)
//        confirmVerified(worker.jobB)
//        confirmVerified(worker.jobC)
    }
}) {
    init {
        taskEngine.avroStorage = taskStorage
        taskEngine.avroDispatcher = taskDispatcher
        taskEngine.logger = logger
        taskMonitoringPerName.avroStorage = taskStorage
        taskMonitoringPerName.avroDispatcher = taskDispatcher
        taskMonitoringPerName.logger = logger
        taskMonitoringGlobal.avroStorage = taskStorage
        taskMonitoringGlobal.logger = logger
        worker.avroDispatcher = taskDispatcher

        decisionEngine.avroStorage = decisionStorage
        decisionEngine.avroDispatcher = decisionDispatcher
        decisionEngine.logger = logger
        decisionMonitoringPerName.avroStorage = decisionStorage
        decisionMonitoringPerName.avroDispatcher = decisionDispatcher
        decisionMonitoringPerName.logger = logger
        decisionMonitoringGlobal.avroStorage = decisionStorage
        decisionMonitoringGlobal.logger = logger
        decider.avroDispatcher = decisionDispatcher

        workflowEngine.avroStorage = workflowStorage
        workflowEngine.avroDispatcher = workflowDispatcher
        workflowEngine.logger = logger
    }
}

private fun getAvroDispatchWorkflow() = AvroConverter.addEnvelopeToWorkflowEngineMessage(
    TestFactory.random(AvroDispatchWorkflow::class, mapOf("workflowName" to "WorkflowA"))
)
