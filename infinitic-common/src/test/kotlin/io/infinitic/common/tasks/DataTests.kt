/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.tasks

import io.infinitic.common.data.Name
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataTests :
    StringSpec({
      "TaskId should be json-serializable" {
        val id = TestFactory.random<String>()
        val m1 = TaskId(id)
        val json = Json.encodeToString(m1)
        val m2 = Json.decodeFromString<TaskId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m1
      }

      "TaskAttemptId should be json-serializable" {
        val id = TestFactory.random<String>()
        val m1 = TaskAttemptId(id)
        val json = Json.encodeToString(m1)
        val m2 = Json.decodeFromString<TaskAttemptId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m1
      }

      "MethodInput should be serialized as List<SerializedData>" {
        val m = MethodParameters.from("a", "b")
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<MethodParameters>(json)

        json shouldBe
            Json.encodeToString(listOf(SerializedData.from("a"), SerializedData.from("b")))
        m2 shouldBe m
      }

      "MethodName should be serialized as String" {
        val id = TestFactory.random<String>()
        val m = MethodName(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<MethodName>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
      }

      "ReturnValue should be serialized as SerializedData" {
        val id = TestFactory.random<String>()
        val m = ReturnValue.from(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ReturnValue>(json)

        json shouldBe Json.encodeToString(SerializedData.from(id))
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
        val id = TestFactory.random<String>()
        val m = TaskAttemptId(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<TaskAttemptId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
      }

      "TaskRetry should be serialized as Int" {
        val m = TaskRetrySequence(42)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<TaskRetrySequence>(json)

        json shouldBe "42"
        m2 shouldBe m
      }

      "TaskAttemptRetry should be serialized as Int" {
        val m = TaskRetryIndex(42)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<TaskRetryIndex>(json)

        json shouldBe "42"
        m2 shouldBe m
      }

      "TaskMeta should be serialized as Map<String, SerializedData>" {
        val m = TaskMeta(mapOf("a" to "1".toByteArray()))
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<TaskMeta>(json)

        m2 shouldBe m
      }

      "TaskName should be serialized as String" {
        val id = TestFactory.random<String>()
        val m = ServiceName(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ServiceName>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
      }

      "Name should be serialized as String" {
        val id = TestFactory.random<String>()
        val m = Name(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<Name>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
      }

      "TaskTak should be stringify as string" {
        val taskTag = TestFactory.random<TaskTag>()

        "$taskTag" shouldBe taskTag.tag
      }
    })
