package io.infinitic.taskManager.common.data

import io.infinitic.common.data.SerializedData

class TaskInputBuilder {
    private var input: MutableList<SerializedData> = mutableListOf()

    fun add(value: Any?): TaskInputBuilder {
        input.add(SerializedData.from(value))

        return this
    }

    fun build() = TaskInput(input)
}
