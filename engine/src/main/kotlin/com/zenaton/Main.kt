package com.zenaton

import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import org.apache.avro.specific.SpecificRecordBase
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

fun main() {
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
