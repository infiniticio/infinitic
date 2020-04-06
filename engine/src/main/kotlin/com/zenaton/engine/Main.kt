package com.zenaton.engine

import com.zenaton.engine.messages.Message
import com.zenaton.engine.messages.WorkflowDispatched
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(Message::class.java)).topic("workflows").create()

    val msg = WorkflowDispatched(
        workflowId = "11dde1dd-ca39-4f67-9fec-0bbc3552e9bf",
        workflowName = "MyHardcodedWorkflowName"
    )

    producer.send(Message(msg))

    producer.close()
    client.close()
}
