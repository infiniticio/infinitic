/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.commands.CommandHash
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandReturnValue
import io.infinitic.common.workflows.data.commands.CommandSimpleName
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.steps.StepHash
import io.infinitic.common.workflows.data.steps.StepId
import io.infinitic.common.workflows.data.steps.StepOutput
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataTests : StringSpec({
    "WorkflowId should be stringify as id" {
        val workflowId = TestFactory.random<WorkflowId>()

        "$workflowId" shouldBe workflowId.id
    }

    "CommandHash should be serialized as String" {
        val m = CommandHash("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<CommandHash>(json)
        m2 shouldBe m
    }

    "CommandId should be serialized as String" {
        val m = CommandId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<CommandId>(json)
        m2 shouldBe m
    }

    "CommandOutput should be serialized as SerializedData" {
        val m = CommandReturnValue.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<CommandReturnValue>(json)
        m2 shouldBe m
    }

    "CommandSimpleName should be serialized as String" {
        val m = CommandSimpleName("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<CommandSimpleName>(json)
        m2 shouldBe m
    }

    "ChannelEvent should be serialized as SerializedData" {
        val m = ChannelEvent(SerializedData.from("qwerty"))

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from("qwerty"))

        val m2 = Json.decodeFromString<ChannelEvent>(json)
        m2 shouldBe m
    }

    "ChannelEventId should be serialized as String" {
        val m = ChannelEventId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<ChannelEventId>(json)
        m2 shouldBe m
    }

    "ChannelEventType should be serialized as String" {
        val m = ChannelEventType("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<ChannelEventType>(json)
        m2 shouldBe m
    }

    "ChannelName should be serialized as String" {
        val m = ChannelName("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<ChannelName>(json)
        m2 shouldBe m
    }

    "MethodRunId should be serialized as String" {
        val m = MethodRunId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<MethodRunId>(json)
        m2 shouldBe m
    }

    "MethodRunPosition should be serialized as String" {
        val m = MethodRunPosition("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<MethodRunPosition>(json)
        m2 shouldBe m
    }

    "PropertyHash should be serialized as String" {
        val m = PropertyHash("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<PropertyHash>(json)
        m2 shouldBe m
    }

    "PropertyName should be serialized as String" {
        val m = PropertyName("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<PropertyName>(json)
        m2 shouldBe m
    }

    "PropertyValue should be serialized as SerializedData" {
        val m = PropertyValue.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<PropertyValue>(json)
        m2 shouldBe m
    }

    "StepHash should be serialized as String" {
        val m = StepHash("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<StepHash>(json)
        m2 shouldBe m
    }

    "StepId should be serialized as String" {
        val m = StepId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<StepId>(json)
        m2 shouldBe m
    }

    "StepOutput should be serialized as SerializedData" {
        val m = StepOutput.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<StepOutput>(json)
        m2 shouldBe m
    }

    "WorkflowId should be serialized as String" {
        val m = WorkflowId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""

        val m2 = Json.decodeFromString<WorkflowId>(json)
        m2 shouldBe m
    }

    "WorkflowMeta should be serialized as Map<String, SerializedData>" {
        val m = WorkflowMeta(mapOf("a" to "1".toByteArray()))

        val json = Json.encodeToString(m)

        val m2 = Json.decodeFromString<WorkflowMeta>(json)
        m2 shouldBe m
    }

    "WorkflowName should be serialized as String" {
        val m = WorkflowName("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<WorkflowName>(json)
        m2 shouldBe m
    }

    "WorkflowTaskId should be serialized as String" {
        val m = WorkflowTaskId("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe "\"qwerty\""
        val m2 = Json.decodeFromString<WorkflowTaskId>(json)
        m2 shouldBe m
    }

    "WorkflowTaskIndex should be serialized as Int" {
        val m = WorkflowTaskIndex(42)

        val json = Json.encodeToString(m)
        json shouldBe "42"
        val m2 = Json.decodeFromString<WorkflowTaskIndex>(json)
        m2 shouldBe m
    }
})
