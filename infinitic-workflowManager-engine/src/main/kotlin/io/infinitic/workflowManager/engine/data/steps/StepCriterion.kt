package io.infinitic.workflowManager.engine.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.workflowManager.engine.data.commands.CommandId
import io.infinitic.workflowManager.data.commands.CommandStatus

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = StepCriterion.Id::class, name = "ID"),
    JsonSubTypes.Type(value = StepCriterion.And::class, name = "AND"),
    JsonSubTypes.Type(value = StepCriterion.Or::class, name = "OR")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class StepCriterion {
    data class Id(val commandId: CommandId, var commandStatus: CommandStatus = CommandStatus.DISPATCHED) : StepCriterion()
    data class And(var commandCriteria: List<StepCriterion>) : StepCriterion()
    data class Or(var commandCriteria: List<StepCriterion>) : StepCriterion()

    @JsonIgnore
    fun isCompleted(): Boolean = when (this) {
        is Id -> this.commandStatus == CommandStatus.COMPLETED
        is And -> this.commandCriteria.all { s -> s.isCompleted() }
        is Or -> this.commandCriteria.any { s -> s.isCompleted() }
    }

    fun complete(commandId: CommandId): StepCriterion {
        when (this) {
            is Id -> if (this.commandId == commandId) this.commandStatus = CommandStatus.COMPLETED
            is And -> this.commandCriteria = this.commandCriteria.map { s -> s.complete(commandId) }
            is Or -> this.commandCriteria = this.commandCriteria.map { s -> s.complete(commandId) }
        }
        return this.resolveOr().compose()
    }

    private fun resolveOr(): StepCriterion {
        when (this) {
            is Id -> Unit
            is And -> this.commandCriteria = this.commandCriteria.map { s -> s.resolveOr() }
            is Or ->
                this.commandCriteria =
                    if (this.isCompleted())
                        listOf(this.commandCriteria.first { s -> s.isCompleted() }.resolveOr())
                    else
                        this.commandCriteria.map { s -> s.resolveOr() }
        }
        return this
    }

    private fun compose(): StepCriterion {
        when (this) {
            is Id -> Unit
            is And -> while (this.commandCriteria.any { s -> s is And || (s is Or && s.commandCriteria.count() == 1) }) {
                this.commandCriteria = this.commandCriteria.fold(mutableListOf<StepCriterion>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.commandCriteria); l }
                        is Or -> { if (s.commandCriteria.count() == 1) l.addAll(s.commandCriteria) else l.add(s); l }
                    }
                }
            }
            is Or -> while (this.commandCriteria.any { s -> s is Or || (s is And && s.commandCriteria.count() == 1) }) {
                this.commandCriteria = this.commandCriteria.fold(mutableListOf<StepCriterion>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { if (s.commandCriteria.count() == 1) l.addAll(s.commandCriteria) else l.add(s); l }
                        is Or -> { l.addAll(s.commandCriteria); l }
                    }
                }
            }
        }
        return this
    }
}
