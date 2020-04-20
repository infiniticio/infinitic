package com.zenaton
import com.zenaton.engine.common.attributes.BranchData
import com.zenaton.engine.common.attributes.DateTime
import com.zenaton.engine.common.attributes.EventName
import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.common.attributes.WorkflowName
import com.zenaton.engine.workflows.WorkflowDispatched
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.workflows.PulsarMessage
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(PulsarMessage::class.java)).topic("workflows").create()

    val wd = WorkflowDispatched(
        workflowId = WorkflowId(),
        workflowName = WorkflowName("MyHardcodedWorkflowName"),
        workflowData = BranchData(ByteArray(10)),
        dispatchedAt = DateTime()
    )
    val b = BranchData("".toByteArray())
    println(b.hash())
//    producer.send(MessageConverter.toPulsar(wd))
    producer.close()
    client.close()
}
