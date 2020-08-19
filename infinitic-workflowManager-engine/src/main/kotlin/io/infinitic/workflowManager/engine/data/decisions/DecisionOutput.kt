package io.infinitic.workflowManager.engine.data.decisions

import io.infinitic.workflowManager.engine.data.properties.Property
import io.infinitic.workflowManager.engine.data.properties.PropertyKey

data class DecisionOutput(
    val updatedProperties: Map<PropertyKey, Property> = mapOf()
)
