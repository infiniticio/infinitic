package com.zenaton.engine.attributes.workflows.states

import com.zenaton.engine.attributes.types.Data

data class PropertyData(override val data: ByteArray) : Data(data) {
    /**
     * @return PropertyHash
     */
    fun propertyHash(): PropertyHash {
        return PropertyHash(super.hash())
    }
}
