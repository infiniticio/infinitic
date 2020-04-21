package com.zenaton.engine.workflows

import com.zenaton.engine.attributes.workflows.WorkflowState

interface StaterInterface {
    fun getState(key: String): WorkflowState?
    fun createState(state: WorkflowState)
    fun updateState(state: WorkflowState)
    fun deleteState(state: WorkflowState)
}
