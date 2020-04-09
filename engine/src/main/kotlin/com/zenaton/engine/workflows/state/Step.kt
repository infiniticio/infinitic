package com.zenaton.engine.workflows.state


sealed class Step {
    data class Id(val id: UnitStepId, var completed: Boolean = false) : Step()
    data class And(val steps: List<Step>) : Step()
    data class Or(val steps: List<Step>) : Step()

    fun isCompleted() : Boolean = when (this) {
        is Id -> this.completed
        is And -> this.steps.all { s -> s.isCompleted() }
        is Or -> this.steps.any { s -> s.isCompleted() }
    }

    fun resolve() : Step = when (this) {
        is Id -> this
        is And -> Step.And(this.steps.map{ s -> s.resolve() })
        is Or -> if (this.isCompleted()) this.steps.first{ s -> s.isCompleted() }.resolve() else this
    }
}
