package com.zenaton
import com.zenaton.engine.attributes.workflows.states.ActionId
import com.zenaton.engine.attributes.workflows.states.Step
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

    fun getStep() = Step.Id(ActionId())

    val stepA = getStep()
    val stepB = getStep()
    val stepC = getStep()
    val step = Step.Or(listOf(stepA, Step.And(listOf(stepB, stepC))))

    var json = Json.to(step)
    println(json)
    var back = Json.from(json, Step::class)
    println(back)
    println(back == step)

//    producer.send(MessageConverter.toPulsar(wd))
    producer.close()
    client.close()
}
