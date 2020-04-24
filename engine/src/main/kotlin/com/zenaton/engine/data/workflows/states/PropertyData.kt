package com.zenaton.engine.data.workflows.states

import com.zenaton.engine.data.types.Data

data class PropertyData(override val data: ByteArray) : Data(data) {
    /**
     * @return PropertyHash
     */
    fun propertyHash(): PropertyHash {
        return PropertyHash(super.hash())
    }
}
