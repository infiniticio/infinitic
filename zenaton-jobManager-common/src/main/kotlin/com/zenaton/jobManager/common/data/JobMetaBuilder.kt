package com.zenaton.jobManager.common.data

import com.zenaton.common.data.SerializedData

class JobMetaBuilder {
    private var meta: MutableMap<String, SerializedData> = mutableMapOf()

    fun add(key: String, data: Any?): JobMetaBuilder {
        meta[key] = SerializedData.from(data)

        return this
    }

    fun build() = JobMeta(meta)
}
