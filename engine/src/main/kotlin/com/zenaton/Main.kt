package com.zenaton

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.data.TaskOutput
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.pulsar.topics.Topic
import com.zenaton.pulsar.topics.workflows.messages.WorkflowMessageContainer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(WorkflowMessageContainer::class.java)).topic("workflows").create()

    var msg = TaskCompleted(
        workflowId = WorkflowId(),
        taskId = TaskId(),
        taskOutput = TaskOutput("oUtput".toByteArray()),
        receivedAt = DateTime()
    )

Topic.TASKS.get() //    println(JSONSchema.of(WorkflowId::class.java))

//    println(AvroMapper().schemaFor(Test::class.java).avroSchema)
//    println(JSONSchema.of(Test::class.java).schemaInfo)
//    val json = Json.stringify(Test("t".toByteArray()))
//    println(json)
//    var msg = Json.parse(json, Test::class) as Test
//    println(msg)
//
//    println(Json.stringify(Store(mapOf(PropertyHash("hhhh") to PropertyData("t".toByteArray())))))
//    println(AvroMapper().schemaFor(WorkflowDispatched::class.java).avroSchema)

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

    producer.send(WorkflowMessageContainer(msg))
    producer.close()
    client.close()
}
