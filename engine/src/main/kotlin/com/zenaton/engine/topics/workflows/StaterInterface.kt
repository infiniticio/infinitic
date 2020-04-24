package com.zenaton.engine.topics.workflows

import com.zenaton.engine.data.workflows.WorkflowState

interface StaterInterface {
    fun getState(key: String): WorkflowState?
    fun createState(state: WorkflowState)
    fun updateState(state: WorkflowState)
    fun deleteState(state: WorkflowState)
}
