package com.zenaton

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.messages.JobStatusUpdated

fun main() {
    AvroConverter.toMonitoringPerName(JobStatusUpdated(
        jobId = JobId(),
        jobName = JobName("ttt"),
        oldStatus = null,
        newStatus = JobStatus.TERMINATED_COMPLETED
    ))
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
