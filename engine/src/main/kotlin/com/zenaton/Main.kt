package com.zenaton

import com.zenaton.engine.data.TaskData
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskName
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.messages.tasks.AvroTaskMessage
import com.zenaton.pulsar.topics.tasks.converter.TaskMessageConverter
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(AvroSchema.of(AvroTaskMessage::class.java)).topic("persistent://public/default/tasks").create()

//    val tad = TaskAttemptDispatched(
//        taskId = TaskId(),
//        taskAttemptId = TaskAttemptId(),
//        taskAttemptIndex = 1,
//        taskName = TaskName("MyTask"),
//        taskData = null
//    )
//    val v = TaskAttemptConverter.toAvro(tad)
//    println(v.toString())
//    println(TaskAttemptConverter.fromAvro(v))

    var msg = TaskDispatched(
        taskId = TaskId(),
        taskName = TaskName("MyTask"),
        taskData = TaskData("abc".toByteArray()),
        workflowId = WorkflowId()
    )

//    val state = TaskState(
//        taskId = msg.taskId,
//        taskName = msg.taskName,
//        taskData = msg.taskData,
//        workflowId = msg.workflowId,
//        taskAttemptId = TaskAttemptId(),
//        taskAttemptIndex = 0
//    )

//    val avroTaskState = AvroConverter.toAvro(state)
//    println(avroTaskState)
//    val byteBuffer = AvroSerDe.serialize(avroTaskState)
//    val n = AvroSerDe.deserialize(byteBuffer, AvroTaskState::class)
//    println(n)
//    val st = AvroConverter.fromAvro(n)
//    println(st)
//    val mapper = AvroMapper()
//    val schema = mapper.schemaFor(TaskState::class.java)
//
//    val baos = ByteArrayOutputStream()
//    mapper.writer(schema).writeValue(baos, state)
//    baos.flush()
//    val byteArray: ByteArray = baos.toByteArray()
//
//    val n = mapper.readerFor(AvroTaskState::class.java).with(schema).readValue<AvroTaskState>(byteArray)
//    println(n)

    producer.send(TaskMessageConverter.toAvro(msg))

    producer.close()
    client.close()
}
