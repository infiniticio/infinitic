package io.infinitic.common.tasks.serializer

import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataTests : StringSpec({
    "MethodInput should be serialized as List<SerializedData>" {
        val m = MethodInput.from("a", "b")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(listOf(SerializedData.from("a"), SerializedData.from("b")))

        val m2 = Json.decodeFromString<MethodInput>(json)
        m2 shouldBe m
    }

    "MethodName should be serialized as String" {
        val m = MethodName("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<MethodName>(json)
        m2 shouldBe m
    }

    "MethodOutput should be serialized as SerializedData" {
        val m = MethodOutput.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<MethodOutput>(json)
        m2 shouldBe m
    }

    "MethodParameterTypes should be serialized as List<String>" {
        val m = MethodParameterTypes(listOf("a", "b", "c"))

        val json = Json.encodeToString(m)
        json shouldBe "[\"a\",\"b\",\"c\"]"

        val m2 = Json.decodeFromString<MethodParameterTypes>(json)
        m2 shouldBe m
    }

    "MethodParameterTypes[null] should be serialized as null" {
        val m = MethodParameterTypes(null)

        val json = Json.encodeToString(m)
        json shouldBe "null"

        val m2 = Json.decodeFromString<MethodParameterTypes>(json)
        m2 shouldBe m
    }

    "TaskAttemptId should be serialized as String" {
        val m = TaskAttemptId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<TaskAttemptId>(json)
        m2 shouldBe m
    }

    "TaskAttemptIndex should be serialized as Int" {
        val m = TaskAttemptIndex(42)

        val json = Json.encodeToString(m)
        json shouldBe "42"

        val m2 = Json.decodeFromString<TaskAttemptIndex>(json)
        m2 shouldBe m
    }

    "TaskAttemptRetry should be serialized as Int" {
        val m = TaskAttemptRetry(42)

        val json = Json.encodeToString(m)
        json shouldBe "42"

        val m2 = Json.decodeFromString<TaskAttemptRetry>(json)
        m2 shouldBe m
    }

    "TaskId should be serialized as String" {
        val m = TaskId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<TaskId>(json)
        m2 shouldBe m
    }

    "TaskMeta should be serialized as Map<String, SerializedData>" {
        val m = TaskMeta.from(mapOf("a" to 1))

        val json = Json.encodeToString(m)
        json shouldBe "{\"a\":${Json.encodeToString(SerializedData.from(1))}}"

        val m2 = Json.decodeFromString<TaskMeta>(json)
        m2 shouldBe m
    }

    "TaskName should be serialized as String" {
        val m = TaskName("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<TaskName>(json)
        m2 shouldBe m
    }
})

