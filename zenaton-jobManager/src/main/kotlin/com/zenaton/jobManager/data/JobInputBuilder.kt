package com.zenaton.jobManager.data

import com.zenaton.common.data.SerializedData

class JobInputBuilder {
    private var input: MutableList<SerializedData> = mutableListOf()

    fun add(value: Any?): JobInputBuilder {
        input.add(SerializedData.from(value))

        return this
    }

    fun build() = JobInput(input)
}
