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

import io.infinitic.common.data.ReturnValue
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.channels.ChannelSignalType
import io.infinitic.common.workflows.data.commands.CommandHash
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandSimpleName
import io.infinitic.common.workflows.data.commands.DispatchWorkflowPastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.steps.StepHash
import io.infinitic.common.workflows.data.steps.StepId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class DataTests : StringSpec({
    "WorkflowId should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = WorkflowId(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<WorkflowId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "CommandHash should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = CommandHash(id)

        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<CommandHash>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "CommandId should be serialized as String" {
        val id = TestFactory.random<String>()
        val m = CommandId(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<CommandId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "CommandOutput should be serialized as SerializedData and reversible in json" {
        val id = TestFactory.random<String>()
        val m = ReturnValue.from(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ReturnValue>(json)

        json shouldBe Json.encodeToString(SerializedData.from(id))
        m2 shouldBe m
    }

    "CommandSimpleName should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = CommandSimpleName(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<CommandSimpleName>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "ChannelSignal should be serialized as SerializedData and reversible in json" {
        val id = TestFactory.random<String>()
        val m = ChannelSignal(SerializedData.from(id))
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ChannelSignal>(json)

        json shouldBe Json.encodeToString(SerializedData.from(id))
        m2 shouldBe m
    }

    "ChannelSignalId should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = ChannelSignalId(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ChannelSignalId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "ChannelSignalType should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = ChannelSignalType(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ChannelSignalType>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "ChannelName should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = ChannelName(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ChannelName>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "MethodRunId should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = MethodRunId(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<MethodRunId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "MethodRunPosition should be serialized as String and reversible in json" {
        val m = MethodRunPosition()
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<MethodRunPosition>(json)

        json shouldBe Json.encodeToString(-1)
        m2 shouldBe m
    }

    "PropertyHash should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = PropertyHash(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<PropertyHash>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "PropertyName should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = PropertyName(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<PropertyName>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "PropertyValue should be serialized as SerializedData and reversible in json" {
        val id = TestFactory.random<String>()
        val m = PropertyValue.from(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<PropertyValue>(json)

        json shouldBe Json.encodeToString(SerializedData.from(id))
        m2 shouldBe m
    }

    "StepHash should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = StepHash(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<StepHash>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "StepId should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = StepId(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<StepId>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "StepOutput should be serialized as SerializedData and reversible in json" {
        val id = TestFactory.random<String>()
        val m = ReturnValue.from(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<ReturnValue>(json)

        json shouldBe Json.encodeToString(SerializedData.from(id))
        m2 shouldBe m
    }

    "WorkflowMeta should be serializable and reversible in json" {
        val id = TestFactory.random<String>()
        val m = WorkflowMeta(mapOf("a" to id.toByteArray()))
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<WorkflowMeta>(json)

        m2 shouldBe m
    }

    "WorkflowName should be serialized as String and reversible in json" {
        val id = TestFactory.random<String>()
        val m = WorkflowName(id)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<WorkflowName>(json)

        json shouldBe Json.encodeToString(id)
        m2 shouldBe m
    }

    "WorkflowTaskIndex should be serialized as Int and reversible in json" {
        val m = WorkflowTaskIndex(42)
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<WorkflowTaskIndex>(json)

        json shouldBe "42"
        m2 shouldBe m
    }

    "DispatchWorkflowPastCommand should reversible in json" {
        val m = TestFactory.random<DispatchWorkflowPastCommand>()
        val json = Json.encodeToString(m)
        val m2 = Json.decodeFromString<DispatchWorkflowPastCommand>(json)

        m2 shouldBe m
    }

    "WorkflowTag should be stringify as string" {
        val workflowTag = TestFactory.random<WorkflowTag>()

        "$workflowTag" shouldBe workflowTag.tag
    }
})
