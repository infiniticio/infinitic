package com.zenaton.workflowManager.data

import com.zenaton.workflowManager.data.properties.Properties
import com.zenaton.workflowManager.data.properties.Property
import com.zenaton.workflowManager.data.properties.PropertyHash
import com.zenaton.workflowManager.data.properties.PropertyKey
import com.zenaton.workflowManager.data.properties.PropertyStore

data class DecisionOutput(
    val updatedProperties: Map<PropertyKey, Property> = mapOf()
)
