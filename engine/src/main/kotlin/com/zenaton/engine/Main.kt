package com.zenaton.engine

import com.zenaton.engine.pulsar.messages.Message
import com.zenaton.engine.tasks.messages.TaskDispatched
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(Message::class.java)).topic("workflows").create()

    val wd = WorkflowDispatched(
        workflowId = "11dde1dd-ca39-4f67-9fec-0bbc3552e9bf",
        workflowName = "MyHardcodedWorkflowName"
    )

    val td = TaskDispatched(
        taskId = "11dde1dd-ca39-4f67-9fec-0bbc3552e9bf",
        taskName = "MyHardcodedWorkflowName"
    )

    producer.send(Message(wd))
    producer.send(Message(td))

    producer.close()
    client.close()
}
