package com.zenaton.workflowManager.avroEngines

import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.avroEngines.AvroMonitoringGlobal
import com.zenaton.jobManager.avroEngines.AvroMonitoringPerName

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.avroEngines.jobSync.SyncWorkerDecision
import com.zenaton.workflowManager.avroEngines.jobSync.SyncDispatcher
import com.zenaton.workflowManager.avroEngines.jobSync.SyncStorage
import com.zenaton.workflowManager.avroEngines.jobSync.SyncWorkerTask
import com.zenaton.workflowManager.messages.AvroDispatchWorkflow
import com.zenaton.workflowManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.slf4j.Logger

private val logger = mockk<Logger>(relaxed = true)

private val taskEngine = AvroJobEngine()
private val taskMonitoringPerName = AvroMonitoringPerName()
private val taskMonitoringGlobal = AvroMonitoringGlobal()
private val worker = SyncWorkerTask()
private val taskDispatcher = SyncDispatcher(taskEngine, taskMonitoringPerName, taskMonitoringGlobal, worker)
private val taskStorage = SyncStorage()

private val decisionEngine = AvroJobEngine()
private val decisionMonitoringPerName = AvroMonitoringPerName()
private val decisionMonitoringGlobal = AvroMonitoringGlobal()
private val decider = SyncWorkerDecision()
private val decisionDispatcher = SyncDispatcher(decisionEngine, decisionMonitoringPerName, decisionMonitoringGlobal, decider)
private val decisionStorage = SyncStorage()

class AvroWorkflowEngineTests : StringSpec({
    "Job succeeds at first try" {
        beforeTest()
        // job will succeed
//        worker.behavior = { SyncWorker.Status.SUCCESS }
//        // run system
//        val dispatch = getAvroDispatchWorkflow()
//        runBlocking {
//            taskDispatcher.scope = this
//            taskDispatcher.toJobEngine(dispatch)
//        }
//        // check that job is completed
//        taskStorage.jobEngineStore.get(dispatch.jobId) shouldBe null
//        // checks number of job processings
//        verify(exactly = 1) {
//            worker.jobA.handle()
//        }
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
    }
}

private fun beforeTest() {
    worker.jobA = mockk()
    every { worker.jobA.handle() } just Runs
    taskStorage.init()
}

private fun getAvroDispatchWorkflow() = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroDispatchWorkflow::class, mapOf("jobName" to "JobA"))
)
