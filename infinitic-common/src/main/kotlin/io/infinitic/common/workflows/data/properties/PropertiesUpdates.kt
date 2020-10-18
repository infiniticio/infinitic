package io.infinitic.common.workflows.data.properties

data class PropertiesUpdates(
    val hashValueUpdates: MutableMap<PropertyHash, PropertyValue>,
    val nameHashUpdates: MutableMap<PropertyName, PropertyHash>
)
