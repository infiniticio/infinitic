package io.infinitic.workflowManager.common

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.utils.TestFactory

fun main() {
    val obj1 = TestFactory.random<DispatchWorkflow>()
    println(obj1)
    val avro1 = AvroConverter.toWorkflowEngine(obj1)
    println(avro1)

//    val obj2 = AvroConverter.fromWorkflowEngine(avro1)
//    println(obj2)
//
//    println(obj1 == obj2)

    val avro2 = AvroConverter.toWorkflowEngine(obj1)
    println(avro2)

    println(avro1 == avro2)
}
