package io.infinitic.workflowManager.common.data.decisions

import io.infinitic.workflowManager.common.data.properties.Property
import io.infinitic.workflowManager.common.data.properties.PropertyKey

data class DecisionOutput(
    val updatedProperties: Map<PropertyKey, Property> = mapOf()
)
