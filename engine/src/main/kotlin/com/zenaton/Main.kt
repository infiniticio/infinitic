package com.zenaton
import com.zenaton.engine.data.workflows.states.Properties
import com.zenaton.engine.data.workflows.states.PropertyData
import com.zenaton.engine.data.workflows.states.PropertyHash
import com.zenaton.engine.data.workflows.states.PropertyKey
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.workflows.PulsarMessage
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.JSONSchema

fun main() {
    val client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
    val producer = client.newProducer(JSONSchema.of(PulsarMessage::class.java)).topic("workflows").create()

//    val wd = WorkflowDispatched(
//        workflowId = WorkflowId(),
//        workflowName = WorkflowName("MyHardcodedWorkflowName"),
//        workflowData = WorkflowData(ByteArray(10)),
//        dispatchedAt = DateTime()
//    )
    var prop = PropertyData("teste".toByteArray())
    println(prop)
    var json = Json.stringify(prop)
    println(json)
    prop = Json.parse(json, PropertyData::class) as PropertyData
    println(prop)

    var key1 = PropertyKey("key1")
    var key2 = PropertyKey("key2")
    var val1 = PropertyHash("hash1")
    var val2 = PropertyHash("hash2")
    println(val1)
    json = Json.stringify(val1)
    println(json)
    val1 = Json.parse(json, PropertyHash::class) as PropertyHash
    println(key1)

    var p = Properties(mapOf(key1 to val1, key2 to val2))
    println(p)
    json = Json.stringify(p)
    println(json)
    p = Json.parse(json, Properties::class) as Properties
    println(p)

//    var msg = TaskCompleted(
//        workflowId = WorkflowId(),
//        taskId = TaskId(),
//        taskOutput = TaskOutput("oUtput".toByteArray())
//    )
//    println(msg)
//    var json = Json.to(msg)
//    println(json)
//    var back = Json.from(json, msg::class)
//    println(back)
//    back = Json.from(json, PulsarMessage::class)
//    println(back)
//    json = Json.to(back)
//    println(json)
//    back = Json.from(json, WorkflowMessage::class)
//    println(back)

//    fun getStep() = StepCriterion.Id(ActionId(TaskId()))
//
//    val stepA = getStep()
//    val stepB = getStep()
//    val stepC = getStep()
//    val step = StepCriterion.Or(listOf(stepA, StepCriterion.And(listOf(stepB, stepC))))
//
//    var json = Json.to(step)
//    println(json)
//    var back = Json.from(json, StepCriterion::class)
//    println(back)
//    println(back == step)

//    producer.send(MessageConverter.toPulsar(wd))
    producer.close()
    client.close()
}
