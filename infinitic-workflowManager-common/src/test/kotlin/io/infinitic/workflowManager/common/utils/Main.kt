package io.infinitic.workflowManager.common.utils

import io.infinitic.common.json.Json
import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.data.commands.AvroPastCommand
import java.util.*

fun main() {
    val obj = TestFactory.random<PastCommand>()

    println(obj)
    println(Json.stringify(obj))

//    println(obj)
    val avro = AvroConverter.convertJson<AvroPastCommand>(obj)
    println(avro)
    val obj2 = AvroConverter.convertJson<PastCommand>(avro)
    println(obj2)
    println(Json.stringify(obj2))

    println(obj == obj2)
}
