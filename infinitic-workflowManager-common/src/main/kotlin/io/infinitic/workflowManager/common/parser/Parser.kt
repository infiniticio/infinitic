package io.infinitic.workflowManager.common.parser

import io.infinitic.workflowManager.common.data.properties.PropertyValue
import io.infinitic.workflowManager.common.data.properties.PropertyName
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

public fun setPropertiesToObject(obj: Any, values: Map<PropertyName, PropertyValue?>) {
    val properties = obj::class.memberProperties
    values.forEach { (name, value) ->
        properties.find { it.name == name.name }
            ?.javaField?.apply {
                val accessible = isAccessible
                if (!accessible) try {
                    isAccessible = true
                } catch (e: SecurityException) {
                    throw Exception("property can not set accessible")
                }
                try {
                    set(obj, value?.data)
                } catch (e: IllegalAccessException) {
                    throw Exception("property is final")
                } catch (e: IllegalArgumentException) {
                    throw Exception("value is not of the property's type")
                } catch (e: Exception) {
                    throw Exception("Impossible to set property")
                }
                if (!accessible) isAccessible = false
            }
            ?: throw Exception("Unknown property ${name.name}")
    }
}
