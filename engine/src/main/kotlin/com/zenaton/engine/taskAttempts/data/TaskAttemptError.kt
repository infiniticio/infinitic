package com.zenaton.engine.taskAttempts.data

import com.zenaton.engine.interfaces.data.DataInterface

data class TaskAttemptError(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
