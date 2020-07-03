package com.zenaton.workflowManager.avroEngines

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.avroEngines.AvroJobEngine
import com.zenaton.jobManager.avroEngines.AvroMonitoringGlobal
import com.zenaton.jobManager.avroEngines.AvroMonitoringPerName
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroJobCompleted
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorker.Status
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorkerDecision
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorkerTask
import com.zenaton.workflowManager.avroEngines.jobInMemory.Task
import com.zenaton.workflowManager.avroEngines.jobInMemory.Workflow
import com.zenaton.workflowManager.avroEngines.workflowInMemory.InMemoryDispatcher
import com.zenaton.workflowManager.avroEngines.workflowInMemory.InMemoryStorage
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.engines.WorkflowEngine
import com.zenaton.workflowManager.messages.AvroDecisionCompleted
import com.zenaton.workflowManager.messages.AvroDispatchWorkflow
import com.zenaton.workflowManager.messages.AvroTaskCompleted
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import com.zenaton.workflowManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger
import com.zenaton.jobManager.avroConverter.AvroConverter as AvroJobConverter
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryDispatcher as InMemoryJobDispatcher
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryStorage as InMemoryJobStorage

private val mockLogger = mockk<Logger>(relaxed = true)

private val taskEngine = AvroJobEngine()
private val taskMonitoringPerName = AvroMonitoringPerName()
private val taskMonitoringGlobal = AvroMonitoringGlobal()
private val worker = InMemoryWorkerTask()
private val taskDispatcher = InMemoryJobDispatcher()
private val taskStorage = InMemoryJobStorage()

private val decisionEngine = AvroJobEngine()
private val decisionMonitoringPerName = AvroMonitoringPerName()
private val decisionMonitoringGlobal = AvroMonitoringGlobal()
private val decider = InMemoryWorkerDecision()
private val decisionDispatcher = InMemoryJobDispatcher()
private val decisionStorage = InMemoryJobStorage()

private val workflowEngine = AvroWorkflowEngine()
private val workflowDispatcher = InMemoryDispatcher()
private val workflowStorage = InMemoryStorage()

class AvroWorkflowEngineTests : StringSpec({
    beforeTest {
        fun getMockTask(): Task { val task = mockk<Task>(); every { task.handle() } just Runs; return task }
        worker.taskA = getMockTask()
        worker.taskB = getMockTask()
        worker.taskC = getMockTask()
        fun getMockWorkflow(): Workflow { val flow = mockk<Workflow>(); every { flow.handle() } just Runs; return flow }
        decider.workflowA = getMockWorkflow()

        taskStorage.init()
    }

    "Task succeeds" {
        // job will succeed only at the 4th try
        worker.behavior = { Status.COMPLETED }
        // run system
        val dispatch = getAvroDispatchTask()
        coroutineScope {
            taskDispatcher.scope = this
            taskDispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        taskStorage.jobEngineStore[dispatch.jobId] shouldBe null
    }

    "Decision succeeds" {
        // job will succeed only at the 4th try
        decider.behavior = { Status.COMPLETED }
        // run system
        val dispatch = getAvroDispatchDecision()
        coroutineScope {
            decisionDispatcher.scope = this
            decisionDispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        decisionStorage.jobEngineStore[dispatch.jobId] shouldBe null
    }

    "Job succeeds at first try" {
        // all jobs will succeed
        worker.behavior = { Status.COMPLETED }
        // run system
        val dispatch = getAvroDispatchWorkflow()
//        coroutineScope {
//            workflowDispatcher.scope = this
//            workflowDispatcher.toWorkflowEngine(dispatch)
//        }
//        // check that workflow     is completed
//        taskStorage.jobEngineStore[dispa      tch.workflowId] shouldBe null
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
        // Tasks
        taskEngine.apply {
            avroStorage = taskStorage
            avroDispatcher = taskDispatcher
            logger = mockLogger
        }
        taskMonitoringPerName.apply {
            avroStorage = taskStorage
            avroDispatcher = taskDispatcher
            logger = mockLogger
        }
        taskMonitoringGlobal.apply {
            avroStorage = taskStorage
            logger = mockLogger
        }
        taskDispatcher.apply {
            jobEngineHandle = { taskEngine.handle(it) }
            monitoringPerNameHandle = { taskMonitoringPerName.handle(it) }
            monitoringGlobalHandle = { taskMonitoringGlobal.handle(it) }
            workerHandle = { worker.handle(it) }
            workflowEngineHandle = { workflowEngine.handle(it) }
            catchJobCompletion = { catchTaskCompletion(it) }
        }
        worker.avroDispatcher = taskDispatcher

        // Decisions
        decisionEngine.apply {
            avroStorage = decisionStorage
            avroDispatcher = decisionDispatcher
            logger = mockLogger
        }
        decisionMonitoringPerName.apply {
            avroStorage = decisionStorage
            avroDispatcher = decisionDispatcher
            logger = mockLogger
        }
        decisionMonitoringGlobal.apply {
            avroStorage = decisionStorage
            logger = mockLogger
        }
        decisionDispatcher.apply {
            jobEngineHandle = { decisionEngine.handle(it) }
            monitoringPerNameHandle = { decisionMonitoringPerName.handle(it) }
            monitoringGlobalHandle = { decisionMonitoringGlobal.handle(it) }
            workerHandle = { decider.handle(it) }
            workflowEngineHandle = { workflowEngine.handle(it) }
            catchJobCompletion = { catchDecisionCompletion(it) }
        }
        decider.avroDispatcher = decisionDispatcher

        // Workflows
        workflowEngine.apply {
            avroStorage = workflowStorage
            avroDispatcher = workflowDispatcher
            logger = mockLogger
        }
        workflowDispatcher.apply {
            workflowEngineHandle = { workflowEngine.handle(it) }
            decisionEngineHandle = { decisionEngine.handle(it) }
            taskEngineHandle = { taskEngine.handle(it) }
        }
    }
}

private fun getAvroDispatchWorkflow() = AvroConverter.addEnvelopeToWorkflowEngineMessage(
    TestFactory.random(AvroDispatchWorkflow::class, mapOf("workflowName" to "WorkflowA"))
)

private fun getAvroDispatchTask() = AvroJobConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(
        AvroDispatchJob::class,
        mapOf(
            "jobName" to "TaskA",
            "jobMeta" to mapOf(WorkflowEngine.META_WORKFLOW_ID to SerializedData.from(WorkflowId().id))
        )
    )
)

private fun getAvroDispatchDecision() = AvroJobConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(
        AvroDispatchJob::class,
        mapOf(
            "jobName" to "WorkflowA",
            "jobMeta" to mapOf(WorkflowEngine.META_WORKFLOW_ID to SerializedData.from(WorkflowId().id))
        )
    )
)

private fun catchTaskCompletion(msg: AvroEnvelopeForJobEngine): AvroEnvelopeForWorkflowEngine? {
    val job = AvroJobConverter.removeEnvelopeFromJobEngineMessage(msg)
    if (job is AvroJobCompleted) return AvroConverter.addEnvelopeToWorkflowEngineMessage(
        AvroTaskCompleted.newBuilder()
            .setTaskId(job.jobId)
            .setTaskOutput(job.jobOutput)
            .setWorkflowId(
                job.jobMeta[WorkflowEngine.META_WORKFLOW_ID]
                    ?.let { AvroJobConverter.fromAvroSerializedData(it) }
                    ?.get()
            )
            .build()
    )

    return null
}

private fun catchDecisionCompletion(msg: AvroEnvelopeForJobEngine): AvroEnvelopeForWorkflowEngine? {
    val job = AvroJobConverter.removeEnvelopeFromJobEngineMessage(msg)
    if (job is AvroJobCompleted) return AvroConverter.addEnvelopeToWorkflowEngineMessage(
        AvroDecisionCompleted.newBuilder()
            .setDecisionId(job.jobId)
            .setDecisionOutput(job.jobOutput)
            .setWorkflowId(
                job.jobMeta[WorkflowEngine.META_WORKFLOW_ID]
                    ?.let { AvroJobConverter.fromAvroSerializedData(it) }
                    ?.get()
            )
            .build()
    )

    return null
}
