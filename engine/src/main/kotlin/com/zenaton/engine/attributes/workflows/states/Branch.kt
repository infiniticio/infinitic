package com.zenaton.engine.attributes.workflows.states

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.attributes.events.EventData
import com.zenaton.engine.attributes.events.EventName
import com.zenaton.engine.attributes.types.DateTime
import com.zenaton.engine.attributes.workflows.WorkflowData

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    Type(value = Branch.Handle::class, name = "HANDLE"),
    Type(value = Branch.OnEvent::class, name = "ON_EVENT")
)
sealed class Branch(
    open val decidedAt: DateTime,
    open val dispatchedAt: DateTime,
    open val propertiesAtStart: Properties = Properties(),
    open val steps: List<Step> = listOf<Step>(),
    open val actions: Set<Action> = setOf<Action>()

) {
    data class Handle(
        val workflowData: WorkflowData?,
        override val decidedAt: DateTime,
        override val dispatchedAt: DateTime = DateTime(),
        override val propertiesAtStart: Properties = Properties(),
        override val steps: List<Step> = listOf<Step>(),
        override val actions: Set<Action> = setOf<Action>()
    ) : Branch(decidedAt, dispatchedAt, propertiesAtStart, steps)

    data class OnEvent(
        val eventName: EventName,
        val eventData: EventData?,
        override val decidedAt: DateTime,
        override val dispatchedAt: DateTime = DateTime(),
        override val propertiesAtStart: Properties = Properties(),
        override val steps: List<Step> = listOf<Step>(),
        override val actions: Set<Action> = setOf<Action>()
    ) : Branch(decidedAt, dispatchedAt, propertiesAtStart, steps)
}
