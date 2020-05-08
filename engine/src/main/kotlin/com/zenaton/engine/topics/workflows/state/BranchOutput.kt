package com.zenaton.engine.topics.workflows.state

import com.zenaton.engine.data.interfaces.DataInterface

data class BranchOutput(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
