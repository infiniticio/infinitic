package com.zenaton
import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.pulsar.workflows.PulsarMessage
import com.zenaton.pulsar.workflows.serializers.MessageConverter
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(PulsarMessage::class.java)).topic("workflows").create()

    val wd = WorkflowDispatched(
        workflowId = WorkflowId(),
        workflowName = "MyHardcodedWorkflowName",
        workflowData = "?"
    )

    producer.send(MessageConverter.toPulsar(wd))
    producer.close()
    client.close()
}
