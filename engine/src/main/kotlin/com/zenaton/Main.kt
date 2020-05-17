package com.zenaton

import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.workflowengine.data.WorkflowId
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(AvroSchema.of(AvroTaskMessage::class.java)).topic("persistent://public/default/tasks").create()

    var msg = DispatchTask(
        taskId = TaskId(),
        taskName = TaskName("MyTask"),
        taskData = TaskData("abc".toByteArray()),
        workflowId = WorkflowId()
    )

    producer.send(TaskAvroConverter.toAvro(msg))

    producer.close()
    client.close()
}
