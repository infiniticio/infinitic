package io.infinitic.workflowManager.common.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.infinitic.workflowManager.data.commands.CommandStatus

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Step.Id::class, name = "ID"),
    JsonSubTypes.Type(value = Step.And::class, name = "AND"),
    JsonSubTypes.Type(value = Step.Or::class, name = "OR")
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Step {
    data class Id(val commandId: CommandId, var status: () -> Status) : Step()
    data class And(var commands: List<Step>) : Step()
    data class Or(var commands: List<Step>) : Step()

    @JsonIgnore
    fun isCompleted(): Boolean = when (this) {
        is Id -> status() in listOf(Status.COMPLETED, Status.CANCELED)
        is And -> this.commands.all { s -> s.isCompleted() }
        is Or -> this.commands.any { s -> s.isCompleted() }
    }

//    fun complete(commandId: CommandId): Step {
//        when (this) {
//            is Id -> if (this.commandId == commandId) this.isTerminated = CommandStatus.COMPLETED
//            is And -> this.commands = this.commands.map { s -> s.complete(commandId) }
//            is Or -> this.commands = this.commands.map { s -> s.complete(commandId) }
//        }
//        return this.resolveOr().compose()
//    }

    private fun resolveOr(): Step {
        when (this) {
            is Id -> Unit
            is And -> this.commands = this.commands.map { s -> s.resolveOr() }
            is Or ->
                this.commands =
                    if (this.isCompleted())
                        listOf(this.commands.first { s -> s.isCompleted() }.resolveOr())
                    else
                        this.commands.map { s -> s.resolveOr() }
        }
        return this
    }

    private fun compose(): Step {
        when (this) {
            is Id -> Unit
            is And -> while (this.commands.any { s -> s is And || (s is Or && s.commands.count() == 1) }) {
                this.commands = this.commands.fold(mutableListOf<Step>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { l.addAll(s.commands); l }
                        is Or -> { if (s.commands.count() == 1) l.addAll(s.commands) else l.add(s); l }
                    }
                }
            }
            is Or -> while (this.commands.any { s -> s is Or || (s is And && s.commands.count() == 1) }) {
                this.commands = this.commands.fold(mutableListOf<Step>()) { l, s ->
                    return@fold when (s) {
                        is Id -> { l.add(s); l }
                        is And -> { if (s.commands.count() == 1) l.addAll(s.commands) else l.add(s); l }
                        is Or -> { l.addAll(s.commands); l }
                    }
                }
            }
        }
        return this
    }

    enum class Status {
        ONGOING,
        CANCELED {
            lateinit var output: Any
        },
        COMPLETED{
            lateinit var output: Any
        }
    }
}
