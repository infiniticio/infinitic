package io.infinitic.taskManager.common.data

import io.infinitic.common.data.SerializedData

class TaskMetaBuilder {
    private var meta: MutableMap<String, SerializedData> = mutableMapOf()

    fun add(key: String, data: Any?): TaskMetaBuilder {
        meta[key] = SerializedData.from(data)

        return this
    }

    fun build() = TaskMeta(meta)
}
