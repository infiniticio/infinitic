package com.zenaton.workflowManager.data

import com.zenaton.workflowManager.data.orders.Order
import com.zenaton.workflowManager.data.properties.Property
import com.zenaton.workflowManager.data.properties.PropertyKey
import com.zenaton.workflowManager.data.steps.Step

data class DecisionOutput(
    val updatedProperties: Map<PropertyKey, Property> = mapOf(),
    val orders: List<Order> = listOf(),
    val step: Step?
)
