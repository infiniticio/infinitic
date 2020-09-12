package io.infinitic.workflowManager.common.data.methods

import io.infinitic.workflowManager.common.data.instructions.PastPosition

data class MethodPosition(
    val parent: MethodPosition?,
    val index: Int
) {
    val pastPosition: PastPosition = when (parent) {
        null -> PastPosition("$index")
        else -> PastPosition("${parent.pastPosition}.$index")
    }

    override fun toString() = "$pastPosition"

    fun next() = MethodPosition(parent, index + 1)

    fun up() = parent

    fun down() = MethodPosition(this, -1)
}
