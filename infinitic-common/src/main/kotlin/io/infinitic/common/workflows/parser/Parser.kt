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

import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

fun <T : Any> setPropertiesToObject(obj: T, values: Map<PropertyName, PropertyValue>) {
    val properties = obj::class.memberProperties
    values.forEach { (name, value) ->
        properties.find { it.name == name.name }
            ?.let { setProperty(obj, it, value.get()) }
            ?: throw RuntimeException("Trying to set unknown property ${obj::class.java.name}:${name.name}")
    }
}

fun <T : Any> getPropertiesFromObject(obj: T, filter: (p: Triple<String, Any?, KType>) -> Boolean = { true }): Map<PropertyName, PropertyValue> =
    obj::class.memberProperties
        .map { p -> Triple(p.name, getProperty(obj, p), p.returnType) }
        .filter { filter(it) }
        .associateBy({ PropertyName(it.first) }, { PropertyValue.from(it.second) })

private fun <T : Any> getProperty(obj: T, kProperty: KProperty1<out T, *>) = kProperty.javaField
    ?.let {
        val errorMsg = "Property ${obj::class.java.name}:${it.name} is not readable"

        val accessible = it.isAccessible
        if (!accessible) try {
            it.isAccessible = true
        } catch (e: SecurityException) {
            throw RuntimeException("$errorMsg (can not set accessible)")
        }
        val value = try {
            it.get(obj)
        } catch (e: Exception) {
            throw RuntimeException("$errorMsg ($e)")
        }
        if (!accessible) it.isAccessible = false

        value
    }

private fun <T : Any> setProperty(obj: T, kProperty: KProperty1<out T, *>, value: Any?) {
    kProperty.javaField?.apply {
        val errorMsg = "Property ${obj::class.java.name}:$name can not be set"

        val accessible = isAccessible
        if (!accessible) try {
            isAccessible = true
        } catch (e: SecurityException) {
            throw RuntimeException("$errorMsg (can not set it as accessible)")
        }
        try {
            set(obj, value)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("$errorMsg can not be set (is final)")
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("$errorMsg can not be set (wrong ${value?.let { it::class.java.name}} type)")
        } catch (e: Exception) {
            throw RuntimeException("$errorMsg ($e)")
        }
        if (!accessible) isAccessible = false
    }
}
