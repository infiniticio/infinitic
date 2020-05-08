package com.zenaton

import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.messages.topics.tasks.AvroTaskDispatched
import com.zenaton.pulsar.utils.Json
import java.nio.ByteBuffer

fun main() {
//    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
//    val producer = client.newProducer(AvroSchema.of(TaskMessageContainer::class.java)).topic("persistent://public/default/tasks").create()

    val avro = AvroTaskDispatched.newBuilder()
        .setTaskId("fb82e2a8-8c52-41b8-9865-babaef0b3235")
        .setSentAt(1588975137)
        .setTaskName("myTask")
        .setTaskData(ByteBuffer.wrap("b1V0cHV0".toByteArray()))
        .setWorkflowId("3a53e888-a508-4e00-86f1-24a656e8fd80")
        .build()

    println(avro.toString())
    val td = Json.parse(avro.toString(), TaskDispatched::class)
    println(td)
    println(Json.parse(Json.stringify(td), AvroTaskDispatched::class))

//    println(AvroSchema.of(TaskMessage::class.java).avroSchema)
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
//    producer.close()
//    client.close()
}
