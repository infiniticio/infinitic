package com.zenaton.workflowManager.topics.workflows.state

data class Store(val properties: Map<PropertyHash, PropertyData> = mapOf())
