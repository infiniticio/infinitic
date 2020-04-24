package com.zenaton.engine.data.workflows.states

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = StepCriterion.Id::class, name = "ID"),
    JsonSubTypes.Type(value = StepCriterion.And::class, name = "AND"),
    JsonSubTypes.Type(value = StepCriterion.Or::class, name = "OR")
)
sealed class StepCriterion {
    data class Id(val actionId: ActionId, var status: ActionStatus = ActionStatus.DISPATCHED) : StepCriterion()
    data class And(var criteria: List<StepCriterion>) : StepCriterion()
    data class Or(var criteria: List<StepCriterion>) : StepCriterion()

    @JsonIgnore
    fun isCompleted(): Boolean = when (this) {
        is Id -> this.status == ActionStatus.COMPLETED
        is And -> this.criteria.all { s -> s.isCompleted() }
        is Or -> this.criteria.any { s -> s.isCompleted() }
    }

    fun complete(actionId: ActionId): StepCriterion {
        when (this) {
            is Id -> if (this.actionId == actionId) this.status = ActionStatus.COMPLETED
            is And -> this.criteria = this.criteria.map { s -> s.complete(actionId) }
            is Or -> this.criteria = this.criteria.map { s -> s.complete(actionId) }
        }
        return this.resolveOr().compose()
    }

    private fun resolveOr(): StepCriterion {
        when (this) {
            is And -> this.criteria = this.criteria.map { s -> s.resolveOr() }
            is Or -> this.criteria =
                if (this.isCompleted())
                    listOf(this.criteria.first { s -> s.isCompleted() }.resolveOr())
                else
                    this.criteria.map { s -> s.resolveOr() }
        }
        return this
    }

    private fun compose(): StepCriterion {
        when (this) {
            is And -> while (this.criteria.any { s -> s is And || (s is Or && s.criteria.count() == 1) }) {
                this.criteria = this.criteria.fold(mutableListOf<StepCriterion>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.criteria); l }
                        is Or -> { if (s.criteria.count() == 1) l.addAll(s.criteria) else l.add(s); l }
                    }
                }
            }
            is Or -> while (this.criteria.any { s -> s is Or || (s is And && s.criteria.count() == 1) }) {
                this.criteria = this.criteria.fold(mutableListOf<StepCriterion>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { if (s.criteria.count() == 1) l.addAll(s.criteria) else l.add(s); l }
                        is Or -> { l.addAll(s.criteria); l }
                    }
                }
            }
        }
        return this
    }
}
