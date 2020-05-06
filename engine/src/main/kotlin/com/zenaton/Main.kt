package com.zenaton

import com.fasterxml.jackson.dataformat.avro.AvroMapper
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskData
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.data.TaskName
import com.zenaton.engine.tasks.messages.TaskDispatched
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.pulsar.topics.tasks.messages.TaskMessageContainer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(AvroSchema.of(TaskMessageContainer::class.java)).topic("persistent://public/default/tasks").create()

    val msg = TaskDispatched(
        workflowId = WorkflowId(),
        taskId = TaskId(),
        taskName = TaskName("myTask"),
        taskData = TaskData("oUtput".toByteArray()),
        receivedAt = DateTime()
    )
    println(AvroMapper().schemaFor(TaskDispatched::class.java).avroSchema)
//    println(Schema.AVRO(TaskId::class.java).schemaInfo)
//    println(AvroMapper().schemaFor(TaskDispatched::class.java).avroSchema)//    println(Json.stringify(TaskMessageContainer(msg)))
//    println(JSONSchema.of(WorkflowMessageContainer::class.java).avroSchema)
//    println(AvroSchema.of(TaskMessageContainer::class.java).avroSchema)
//    var msg = DecisionDispatched(
//        decisionId = DecisionId(),
//        workflowId = WorkflowId(),
//        workflowName = WorkflowName("testW"),
//        branches = listOf(),
//        store = Store(mapOf(PropertyHash("errt") to PropertyData("dd".toByteArray())))
//    )

//    producer.send(TaskMessageContainer(msg))
    producer.close()
    client.close()
}
