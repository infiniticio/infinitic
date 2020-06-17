package com.zenaton.jobManager

import com.zenaton.commons.data.AvroSerializedData
import com.zenaton.commons.data.AvrosSerializationType
import com.zenaton.commons.json.Json
import com.zenaton.jobManager.data.JobParameter

fun main() {
    val p1 = JobParameter("fdggsdf".toByteArray(), AvrosSerializationType.JSON)
    println(p1)

    val p2 = Json.parse<AvroSerializedData>(Json.stringify(p1))
    println(p2)

    val p3 = Json.parse<JobParameter>(Json.stringify(p2))
    println(p3 == p1)

    val p4 = Json.parse<AvroSerializedData>(Json.stringify(p3))
    println(p4 == p2)
    //    Assert.assertTrue(SchemaCompatibility.schemaNameEquals(newSchema, oldSchema))
    //    Assert.assertNotNull(compatResult)
    //    Assert.assertEquals(
    //        SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE,
    //        compatResult.getType()
    //    )
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
