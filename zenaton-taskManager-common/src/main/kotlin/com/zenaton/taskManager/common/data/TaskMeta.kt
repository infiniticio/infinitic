package com.zenaton.taskManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.interfaces.MetaInterface

data class TaskMeta
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val meta: MutableMap<String, SerializedData> = mutableMapOf()) : MetaInterface {
    companion object {
        const val META_PARAMETER_TYPES = "javaParameterTypes"

        fun builder() = TaskMetaBuilder()
    }

    fun setParameterTypes(types: List<String>?, override: Boolean = false): TaskMeta {
        if (override || !meta.containsKey(META_PARAMETER_TYPES)) {
            types?.let {
                meta[META_PARAMETER_TYPES] = SerializedData.from(types)
            }
        }

        return this
    }

    fun getParameterTypes() = meta[META_PARAMETER_TYPES]?.let {
        @Suppress("UNCHECKED_CAST")
        it.deserialize(List::class.java) as List<String>
    }
}
