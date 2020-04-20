package com.zenaton.engine.workflows.state

data class Action(
    val type: ActionType,
    val id: ActionId,
    val output: String?
)
