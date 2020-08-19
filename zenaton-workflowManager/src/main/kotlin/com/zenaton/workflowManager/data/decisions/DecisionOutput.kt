package com.zenaton.workflowManager.data.decisions

import com.zenaton.workflowManager.data.properties.Property
import com.zenaton.workflowManager.data.properties.PropertyKey

data class DecisionOutput(
    val updatedProperties: Map<PropertyKey, Property> = mapOf()
)
