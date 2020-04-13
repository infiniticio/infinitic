package com.zenaton.engine.workflows.state

sealed class Step {
    data class Id(val id: ActionId, var completed: Boolean = false) : Step()
    data class And(var steps: List<Step>) : Step()
    data class Or(var steps: List<Step>) : Step()

    fun isCompleted(): Boolean = when (this) {
        is Id -> this.completed
        is And -> this.steps.all { s -> s.isCompleted() }
        is Or -> this.steps.any { s -> s.isCompleted() }
    }

    fun complete(actionId: ActionId): Step {
        when (this) {
            is Id -> if (this.id == actionId) this.completed = true
            is And -> this.steps = this.steps.map { s -> s.complete(actionId) }
            is Or -> this.steps = this.steps.map { s -> s.complete(actionId) }
        }
        return this.resolveOr().compose()
    }

    private fun resolveOr(): Step {
        when (this) {
            is And -> this.steps = this.steps.map { s -> s.resolveOr() }
            is Or -> this.steps =
                if (this.isCompleted())
                    listOf(this.steps.first { s -> s.isCompleted() }.resolveOr())
                else
                    this.steps.map { s -> s.resolveOr() }
        }
        return this
    }

    private fun compose(): Step {
        when (this) {
            is And -> while (this.steps.any { s -> s is And || (s is Or && s.steps.count() == 1) }) {
                this.steps = this.steps.fold(mutableListOf<Step>()) { l, s ->
                    when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.steps); l }
                        is Or -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
                    }
                }
            }
            is Or -> while (this.steps.any { s -> s is Or || (s is And && s.steps.count() == 1) }) {
                this.steps = this.steps.fold(mutableListOf<Step>()) { l, s ->
                    when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { if (s.steps.count() == 1) l.addAll(s.steps) else l.add(s); l }
                        is Or -> { l.addAll(s.steps); l }
                    }
                }
            }
        }
        return this
    }
}
