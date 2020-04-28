package com.zenaton.engine.data.workflows.states

import com.zenaton.engine.data.types.Data

data class PropertyData(override val data: ByteArray) : Data(data) {
    fun propertyHash(): PropertyHash {
        return PropertyHash(super.hash())
    }
}
