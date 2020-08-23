package io.infinitic.taskManager.common.data

import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.exceptions.CantUseJavaParameterTypesInMeta
import java.lang.reflect.Method

abstract class Meta(open val meta: MutableMap<String, SerializedData> = mutableMapOf()) {
    companion object {
        const val META_PARAMETER_TYPES = "javaParameterTypes"
    }

    val parameterTypes: List<String>?
        get() = meta[META_PARAMETER_TYPES]?.let {
            @Suppress("UNCHECKED_CAST")
            it.deserialize(List::class.java) as List<String>?
        }

    fun <T : Meta> with(key: String, data: Any?): T {
        if (key == META_PARAMETER_TYPES) throw CantUseJavaParameterTypesInMeta(META_PARAMETER_TYPES)

        meta[key] = SerializedData.from(data)

        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun <T : Meta> withParametersTypesFrom(method: Method): T = withParameterTypes(method.parameterTypes.map { it.name })

    fun <T : Meta> withParameterTypes(p: List<String>?): T {
        if (p != null) {
            if (meta.containsKey(META_PARAMETER_TYPES)) throw CantUseJavaParameterTypesInMeta(META_PARAMETER_TYPES)
            meta[META_PARAMETER_TYPES] = SerializedData.from(p)
        }

        @Suppress("UNCHECKED_CAST")
        return this as T
    }
}
