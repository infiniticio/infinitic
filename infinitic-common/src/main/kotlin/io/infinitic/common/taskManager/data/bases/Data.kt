package io.infinitic.common.taskManager.data.bases

import io.infinitic.common.data.SerializedData

abstract class Data(open val data: Any?) {
    lateinit var serializedData: SerializedData

    fun getSerialized() = when {
        this::serializedData.isInitialized -> serializedData
        else -> SerializedData.from(data)
    }
}
