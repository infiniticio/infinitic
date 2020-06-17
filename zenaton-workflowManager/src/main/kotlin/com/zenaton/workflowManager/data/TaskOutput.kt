package com.zenaton.workflowManager.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenaton.commons.data.interfaces.DataInterface

<<<<<<< HEAD
data class TaskOutput
=======
<<<<<<< HEAD:zenaton-workflowManager/src/main/kotlin/com/zenaton/workflowManager/data/DecisionOutput.kt
data class DecisionOutput
=======
data class TaskOutput
>>>>>>> bb67ede1d33a14a6c823e643574304af0e4058ab:zenaton-workflowManager/src/main/kotlin/com/zenaton/workflowManager/data/TaskOutput.kt
>>>>>>> bb67ede1d33a14a6c823e643574304af0e4058ab
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
constructor(@get:JsonValue override val data: ByteArray) : DataInterface {
    final override fun equals(other: Any?) = equalsData(other)
    final override fun hashCode() = hashCodeData()
}
