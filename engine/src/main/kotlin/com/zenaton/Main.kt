package com.zenaton

import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.topics.workflows.TaskCompleted
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessage
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessageConverter
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(PulsarWorkflowMessage::class.java)).topic("workflows").create()

    var msg = TaskCompleted(
        workflowId = WorkflowId(),
        taskId = TaskId(),
        taskOutput = TaskOutput("oUtput".toByteArray())
    )

//    var msg = WorkflowDispatched(
//        workflowId = WorkflowId(),
//        workflowName = WorkflowName("testW"),
//        workflowData = null,
//        dispatchedAt = DateTime()
//    )

//    var msg = DecisionDispatched(
//        decisionId = DecisionId(),
//        workflowId = WorkflowId(),
//        workflowName = WorkflowName("testW"),
//        branches = listOf(),
//        store = Store(mapOf(PropertyHash("errt") to PropertyData("dd".toByteArray())))
//    )
//    val json = Json.stringify(msg)
//    println(json)
//    msg = Json.parse(json, DecisionDispatched::class) as DecisionDispatched
//    println(msg)

    producer.send(PulsarWorkflowMessageConverter.toPulsar(msg))
    producer.close()
    client.close()
}
