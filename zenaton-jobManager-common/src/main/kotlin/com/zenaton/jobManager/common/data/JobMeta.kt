package com.zenaton.jobManager.common.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.interfaces.MetaInterface

data class JobMeta
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val meta: Map<String, SerializedData> = mapOf()) : MetaInterface {
    companion object {
        const val META_PARAMETER_TYPES = "javaParameterTypes"

        fun builder() = JobMetaBuilder()

        fun forParameterTypes(types: List<String>) = builder().add(META_PARAMETER_TYPES, types).build()
    }

    fun getParameterTypes() = meta[META_PARAMETER_TYPES]
        ?.let {
            @Suppress("UNCHECKED_CAST")
            it.deserialize(List::class.java) as List<String>
        }
}
