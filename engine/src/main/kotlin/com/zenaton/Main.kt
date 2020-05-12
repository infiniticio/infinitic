package com.zenaton

fun main() {
//    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
//    val producer = client.newProducer(AvroSchema.of(AvroTaskMessage::class.java)).topic("persistent://public/default/tasks").create()
//
//    var msg = TaskDispatched(
//        taskId = TaskId(),
//        taskName = TaskName("MyTask"),
//        taskData = TaskData("abc".toByteArray()),
//        workflowId = WorkflowId()
//    )
//
//    producer.send(TaskMessageConverter.toAvro(msg))
//
//    producer.close()
//    client.close()
}
