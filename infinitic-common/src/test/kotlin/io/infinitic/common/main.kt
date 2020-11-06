package io.infinitic.common

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.serDe.kotlin.writeBinary
import io.infinitic.common.workflows.data.steps.Step
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.serializer

@InternalSerializationApi
fun main() {

    val msg = TestFactory.steps()["OR B"] as Step.Or
    println(msg)
    println(Avro.default.schema(Step.Or.serializer()).toString(true))
//
    val b = writeBinary(msg, Step.Or.serializer())

    val d = readBinary(b, Step.Or.serializer())
    println(d)
    println(d == msg)
//    val s = MethodInput.from("a")

//    println(Avro.default.schema(String.serializer()))
//
//    println(Avro.default.encodeToByteArray(String.serializer(), "foo"))
//    println(writeBinary("test", String.serializer()))
//
//    val s = SerializedData.from("qwerty")
//    println(s)
//    val json = Json.encodeToString(s)
//    println(json)
//    val s2 = Json.decodeFromString(SerializedData.serializer(), json)
//    println(s2)
//    println(s2.deserialize())
}
