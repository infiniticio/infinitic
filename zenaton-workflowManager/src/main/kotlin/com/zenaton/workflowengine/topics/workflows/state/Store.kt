package com.zenaton.workflowengine.topics.workflows.state

data class Store(val properties: Map<PropertyHash, PropertyData> = mapOf())
