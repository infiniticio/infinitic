package com.zenaton.engine.workflows

interface StaterInterface {
    fun getState(key: String): WorkflowState?
    fun createState(state: WorkflowState)
    fun updateState(state: WorkflowState)
    fun deleteState(state: WorkflowState)
}
