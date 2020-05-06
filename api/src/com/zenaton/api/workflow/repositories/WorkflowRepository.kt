package com.zenaton.api.workflow.repositories

import com.zenaton.api.workflow.models.Workflow

interface WorkflowRepository {
    fun getById(id: String): Workflow
}
