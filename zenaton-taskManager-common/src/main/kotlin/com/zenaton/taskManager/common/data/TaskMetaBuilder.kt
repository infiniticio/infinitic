package com.zenaton.taskManager.common.data

import com.zenaton.common.data.SerializedData

class TaskMetaBuilder {
    private var meta: MutableMap<String, SerializedData> = mutableMapOf()

    fun add(key: String, data: Any?): TaskMetaBuilder {
        meta[key] = SerializedData.from(data)

        return this
    }

    fun build() = TaskMeta(meta)
}
