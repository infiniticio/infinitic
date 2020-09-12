package io.infinitic.workflowManager.common.data.methods

import io.infinitic.workflowManager.common.data.instructions.StringPosition

data class MethodPosition(
    val parent: MethodPosition?,
    val index: Int
) {
    val stringPosition: StringPosition = when (parent) {
        null -> StringPosition("$index")
        else -> StringPosition("${parent.stringPosition}.$index")
    }

    override fun toString() = "$stringPosition"

    fun next() = MethodPosition(parent, index + 1)

    fun up() = parent

    fun down() = MethodPosition(this, -1)
}
