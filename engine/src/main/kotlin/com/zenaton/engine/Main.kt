package com.zenaton.engine

import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.pulsar.messages.Message
import com.zenaton.engine.workflows.Message.WorkflowDispatched
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(Message::class.java)).topic("workflows").create()

    val wd = WorkflowDispatched(
        workflowId = WorkflowId(),
        workflowName = "MyHardcodedWorkflowName",
        workflowData = "?"
    )
//
//    val td = TaskDispatched(
//        taskId = TaskId(),
//        taskName = "MyHardcodedWorkflowName",
//        workflowId = null
//    )
//
    producer.send(Message(wd))
//    producer.send(Message(td))

    producer.close()
    client.close()
}
