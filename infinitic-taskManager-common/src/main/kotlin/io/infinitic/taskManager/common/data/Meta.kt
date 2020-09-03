package io.infinitic.taskManager.common.data

import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.exceptions.CanNotUseJavaReservedKeywordInMeta
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
