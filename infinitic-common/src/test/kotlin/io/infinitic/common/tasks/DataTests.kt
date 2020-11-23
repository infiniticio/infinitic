// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.tasks

import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskRetry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataTests : StringSpec({
    "TaskId should be stringify as id" {
        val taskId = TestFactory.random<TaskId>()

        "$taskId" shouldBe taskId.id
    }

    "TaskAttemptId should be stringify as id" {
        val taskAttemptId = TestFactory.random<TaskAttemptId>()

        "$taskAttemptId" shouldBe taskAttemptId.id
    }

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

    "TaskAttemptId should be serialized as String" {
        val m = TaskAttemptId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<TaskAttemptId>(json)
        m2 shouldBe m
    }

    "TaskRetry should be serialized as Int" {
        val m = TaskRetry(42)

        val json = Json.encodeToString(m)
        json shouldBe "42"

        val m2 = Json.decodeFromString<TaskRetry>(json)
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
