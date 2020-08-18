package com.zenaton.workflowManager.avroEngines.jobInMemory

import com.zenaton.jobManager.data.AvroSerializedData
import com.zenaton.jobManager.common.avro.AvroConverter as AvroJobConverter
import com.zenaton.jobManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.avroEngines.jobInMemory.InMemoryWorker.Status
import com.zenaton.workflowManager.data.DecisionOutput
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.properties.PropertyStore
import com.zenaton.workflowManager.decisions.AvroDecisionInput

internal class InMemoryWorkerDecision : InMemoryWorker {
    override lateinit var avroDispatcher: AvroDispatcher
    lateinit var behavior: (msg: AvroRunJob) -> Status
    lateinit var workflowA: WorkflowA

    override fun handle(msg: AvroEnvelopeForWorker) {
        when (val avro = AvroJobConverter.removeEnvelopeFromWorkerMessage(msg)) {
            is AvroRunJob -> {
                sendJobStarted(avro)
                val workflow = when (avro.jobName) {
                    "WorkflowA" -> WorkflowA()
                    else -> throw Exception("Unknown job ${avro.jobName}")
                }
                val input = getDecisionInput(avro.jobInput[0])
                val out = runDecision(workflow, input.branches, input.store)
//                sendJobCompleted(avro, AvroConverter.toAvroDecisionOutput(out))
            }
        }
    }

    private fun runDecision(workflow: Workflow, branches: List<Branch>, properties: PropertyStore): DecisionOutput? {
        return null
    }

    private fun getDecisionInput(data: AvroSerializedData) = AvroConverter.fromAvroDecisionInput(
        AvroJobConverter.fromAvroSerializedData(data).deserialize(AvroDecisionInput::class.java) as AvroDecisionInput
    )
}
