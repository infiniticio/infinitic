package com.zenaton.engine.decisionAttempts.data

import com.zenaton.engine.interfaces.data.DataInterface

data class DecisionAttemptError(override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
