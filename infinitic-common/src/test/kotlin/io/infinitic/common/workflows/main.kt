package io.infinitic.common.workflows

import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.parser.getPropertiesFromObject
import java.lang.RuntimeException

fun main() {
    val obj1 = Test1("e", 4)
    val obj2 = Test2("e", 4)

    val previousPropertiesNameValue = getPropertiesFromObject(obj1)
    val previousPropertiesHashValue = previousPropertiesNameValue.mapKeys { it.value.hash() }
    val previousPropertiesNameHash = previousPropertiesNameValue.mapValues { it.value.hash() }

    val currentPropertiesNameValue = getPropertiesFromObject(obj2)

    println(previousPropertiesNameValue)
    println(currentPropertiesNameValue)

    val unknownProperties = previousPropertiesNameValue.keys.filter { it !in currentPropertiesNameValue.keys }.joinToString()
    if (unknownProperties.isNotEmpty()) throw RuntimeException(unknownProperties)

    val hashValueUpdates = mutableMapOf<PropertyHash, PropertyValue>()
    val nameHashUpdates = mutableMapOf<PropertyName, PropertyHash>()

    currentPropertiesNameValue.map {
        val hash = it.value.hash()
        if (it.key !in previousPropertiesNameHash.keys || hash != previousPropertiesNameHash[it.key]) {
            // new property
            nameHashUpdates[it.key] = hash
        }
        if (hash !in hashValueUpdates.keys) {
            hashValueUpdates[hash] = it.value
        }
    }

    println(hashValueUpdates)
    println(nameHashUpdates)

}

data class Test1(val o1: String, private val o2:Int)
data class Test2(val o1: String, private val o2:Int) {
    lateinit var o3 : String
}
