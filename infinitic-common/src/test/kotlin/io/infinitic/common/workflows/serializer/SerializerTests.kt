package io.infinitic.common.workflows.serializer

import io.infinitic.common.data.SerializedData
import io.infinitic.common.workflows.data.commands.CommandOutput
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.steps.StepOutput
import io.infinitic.common.workflows.data.workflows.WorkflowOutput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SerializerTests : StringSpec({


    "CommandOutput should be serialized as SerializedData" {
        val m = CommandOutput.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<CommandOutput>(json)
        m2 shouldBe m
    }


    "PropertyValue should be serialized as SerializedData" {
        val m = PropertyValue.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<PropertyValue>(json)
        m2 shouldBe m
    }

    "StepOutput should be serialized as SerializedData" {
        val m = StepOutput.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<StepOutput>(json)
        m2 shouldBe m
    }

    "WorkflowOutput should be serialized as SerializedData" {
        val m = WorkflowOutput.from("qwerty")

        val json = Json.encodeToString(m)
        json shouldBe Json.encodeToString(SerializedData.from(m.get()))

        val m2 = Json.decodeFromString<WorkflowOutput>(json)
        m2 shouldBe m
    }

})

