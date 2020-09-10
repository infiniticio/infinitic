package io.infinitic.workflowManager.common.data.methods

import io.infinitic.workflowManager.common.data.instructions.Position

data class MethodPosition(
    val parent: MethodPosition?,
    val index: Int
) {
    val position: Position = when (parent) {
        null -> Position("$index")
        else -> Position("${parent.position}.$index")
    }

    override fun toString() = "$position"

    fun next() = MethodPosition(parent, index + 1)

    fun up() = parent

    fun down() = MethodPosition(this, -1)
}
