package io.infinitic.taskManager.common.data

import io.infinitic.common.data.SerializedData

abstract class Error(open val data: Any?) {
    lateinit var serializedData: SerializedData

    fun getSerialized() = when {
        this::serializedData.isInitialized -> serializedData
        else -> SerializedData.from(data)
    }
}
