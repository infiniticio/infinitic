package com.zenaton

import com.zenaton.engine.data.TaskData
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskName
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.messages.topics.tasks.AvroTaskMessage
import com.zenaton.pulsar.topics.tasks.converter.TaskConverter
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(AvroSchema.of(AvroTaskMessage::class.java)).topic("persistent://public/default/tasks").create()

    var msg = TaskDispatched(
        taskId = TaskId(),
        taskName = TaskName("MyTask"),
        taskData = TaskData("abc".toByteArray()),
        workflowId = WorkflowId()
    )
    producer.send(TaskConverter.toAvro(msg))
    
    producer.close()
    client.close()
}
