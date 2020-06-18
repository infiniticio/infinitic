package com.zenaton.workflowManager.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.workflowManager.data.actions.ActionId
import com.zenaton.workflowManager.data.actions.ActionStatus

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = StepCriterion.Id::class, name = "ID"),
    JsonSubTypes.Type(value = StepCriterion.And::class, name = "AND"),
    JsonSubTypes.Type(value = StepCriterion.Or::class, name = "OR")
)
sealed class StepCriterion {
    data class Id(val actionId: ActionId, var actionStatus: ActionStatus = ActionStatus.DISPATCHED) : StepCriterion()
    data class And(var actionCriteria: List<StepCriterion>) : StepCriterion()
    data class Or(var actionCriteria: List<StepCriterion>) : StepCriterion()

    @JsonIgnore
    fun isCompleted(): Boolean = when (this) {
        is Id -> this.actionStatus == ActionStatus.COMPLETED
        is And -> this.actionCriteria.all { s -> s.isCompleted() }
        is Or -> this.actionCriteria.any { s -> s.isCompleted() }
    }

    fun complete(actionId: ActionId): StepCriterion {
        when (this) {
            is Id ->
                if (this.actionId == actionId) this.actionStatus =
                    ActionStatus.COMPLETED
            is And -> this.actionCriteria = this.actionCriteria.map { s -> s.complete(actionId) }
            is Or -> this.actionCriteria = this.actionCriteria.map { s -> s.complete(actionId) }
        }
        return this.resolveOr().compose()
    }

    private fun resolveOr(): StepCriterion {
        when (this) {
            is And -> this.actionCriteria = this.actionCriteria.map { s -> s.resolveOr() }
            is Or ->
                this.actionCriteria =
                    if (this.isCompleted())
                        listOf(this.actionCriteria.first { s -> s.isCompleted() }.resolveOr())
                    else
                        this.actionCriteria.map { s -> s.resolveOr() }
        }
        return this
    }

    private fun compose(): StepCriterion {
        when (this) {
            is And -> while (this.actionCriteria.any { s -> s is And || (s is Or && s.actionCriteria.count() == 1) }) {
                this.actionCriteria = this.actionCriteria.fold(mutableListOf<StepCriterion>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.actionCriteria); l }
                        is Or -> { if (s.actionCriteria.count() == 1) l.addAll(s.actionCriteria) else l.add(s); l }
                    }
                }
            }
            is Or -> while (this.actionCriteria.any { s -> s is Or || (s is And && s.actionCriteria.count() == 1) }) {
                this.actionCriteria = this.actionCriteria.fold(mutableListOf<StepCriterion>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { if (s.actionCriteria.count() == 1) l.addAll(s.actionCriteria) else l.add(s); l }
                        is Or -> { l.addAll(s.actionCriteria); l }
                    }
                }
            }
        }
        return this
    }
}
