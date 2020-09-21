package io.infinitic.workflowManager.common.utils

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.data.steps.AvroStep
import java.util.*

fun main() {
    val obj = TestFactory.randomStep()

//    println(step)
//    println(Json.stringify(step))

    println(obj)
    val avro = AvroConverter.convertJson<AvroStep>(obj)
    println(avro)
    val obj2 = AvroConverter.convertJson<Step>(avro)
    println(obj2)
    println(obj == obj2)
}
