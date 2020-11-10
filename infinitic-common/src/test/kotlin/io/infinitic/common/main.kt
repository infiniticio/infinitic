package io.infinitic.common

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.serDe.kotlin.getKSerializerOrNull
import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.serDe.kotlin.writeBinary
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.exceptions.ExceptionDuringKotlinDeserialization
import io.infinitic.common.tasks.exceptions.SerializerNotFoundDuringDeserialization
import io.infinitic.common.tasks.states.TaskState
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.exceptions.NoMethodCallAtAsync
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.kotest.matchers.shouldBe
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import java.lang.RuntimeException
import java.lang.reflect.Modifier.isStatic
import java.util.*
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

fun main() {

   val e = TestFactory.random<WorkflowTaskInput>()

    val s = SerializedData.from(e)

    println(s.type)
    print(s)

//    val m2 = m.deepCopy()


//    println(m == m2)
//    val envelope = WorkflowEngineEnvelope.from(m)
//    val ser = NewStep.serializer()
//    val byteArray = Avro.default.encodeToByteArray(ser, m)
//    val envelope2 = Avro.default.decodeFromByteArray(ser, byteArray)
//    println(m == envelope2)
//
//
//    val msg = TestFactory.steps()["OR B"] as Step.Or
//    println(msg)
//    println(Avro.default.schema(Step.Or.serializer()).toString(true))
////
//    val b = writeBinary(msg, Step.Or.serializer())
//
//    val d = readBinary(b, Step.Or.serializer())
//    println(d)
//    println(d == msg)
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
