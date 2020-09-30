package io.infinitic.common

import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.tasks.utils.TestFactory
import io.kotest.matchers.shouldBe

fun main() {
    val msg = TestFactory.random(DispatchTask::class)
    println(msg)
    val avroMsg = AvroConverter.toTaskEngine(msg)
    val msg2 = AvroConverter.fromTaskEngine(avroMsg)
    val avroMsg2 = AvroConverter.toTaskEngine(msg2)
    msg shouldBe msg2
    println(avroMsg)
    println(avroMsg2)
    println(avroMsg == avroMsg2)
}
