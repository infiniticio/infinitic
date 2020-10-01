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

package io.infinitic.common.tasks.data.bases

import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.exceptions.CanNotUseJavaReservedKeywordInMeta
import java.lang.reflect.Method

abstract class Meta(open val data: MutableMap<String, Any?> = mutableMapOf()) {
    lateinit var serializedData: Map<String, SerializedData>

    @Suppress("UNCHECKED_CAST")
    val parameterTypes get() = data[META_PARAMETER_TYPES] as List<String>?

    companion object {
        const val META_PARAMETER_TYPES = "javaParameterTypes"

        fun deserialize(serialized: Map<String, SerializedData>) =
            serialized.mapValues { it.value.deserialize() } as MutableMap<String, Any?>
    }

    fun getSerialized() = when {
        this::serializedData.isInitialized -> serializedData
        else -> data.mapValues { SerializedData.from(it.value) }
    }

    fun <T : Meta> with(key: String, data: Any?): T {
        if (key == META_PARAMETER_TYPES) throw CanNotUseJavaReservedKeywordInMeta(META_PARAMETER_TYPES)

        this.data[key] = data

        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun <T : Meta> withParametersTypesFrom(method: Method): T = withParameterTypes(method.parameterTypes.map { it.name })

    fun <T : Meta> withParameterTypes(p: List<String>?): T {
        if (p != null) {
            if (data.containsKey(META_PARAMETER_TYPES)) throw CanNotUseJavaReservedKeywordInMeta(META_PARAMETER_TYPES)
            data[META_PARAMETER_TYPES] = p
        }

        @Suppress("UNCHECKED_CAST")
        return this as T
    }
}
