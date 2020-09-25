package io.infinitic.taskManager.pulsar

fun main(args: Array<String>) {
//    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
//    val producer = client.newProducer(AvroSchema.of(AvroTaskEngineMessage::class.java)).topic("persistent://public/default/tasks").create()
//
//    var msg = DispatchTask(
//        taskId = TaskId(),
//        taskName = TaskName("MyTask"),
//        taskData = TaskData("abc".toByteArray()),
//        workflowId = WorkflowId()
//    )
//
//    producer.send(TaskAvroConverter.toAvro(msg))
//
//    producer.close()
//    client.close()
}
