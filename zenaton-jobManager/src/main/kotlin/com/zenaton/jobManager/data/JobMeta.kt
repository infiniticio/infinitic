package com.zenaton.jobManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.interfaces.MetaInterface

data class JobMeta
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val meta: Map<String, SerializedData> = mapOf()) : MetaInterface {
    companion object {
        fun builder() = JobMetaBuilder()
    }
}
