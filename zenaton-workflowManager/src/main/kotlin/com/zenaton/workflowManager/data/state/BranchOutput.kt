package com.zenaton.workflowManager.data.state

import com.zenaton.commons.data.interfaces.DataInterface

data class BranchOutput(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
