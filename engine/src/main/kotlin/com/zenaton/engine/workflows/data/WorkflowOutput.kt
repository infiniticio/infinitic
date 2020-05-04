package com.zenaton.engine.workflows.data

import com.zenaton.engine.interfaces.data.DataInterface

data class WorkflowOutput(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
