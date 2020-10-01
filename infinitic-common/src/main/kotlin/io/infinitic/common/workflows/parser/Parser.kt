// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.parser

import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.properties.PropertyName
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
