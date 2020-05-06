package com.zenaton.api

import com.zenaton.api.workflow.repositories.PrestoJdbcWorkflowRepository
import com.zenaton.api.workflow.repositories.WorkflowRepository
import org.koin.dsl.module
import org.koin.experimental.builder.singleBy

val ApiModule = module(createdAtStart = true) {
    singleBy<WorkflowRepository, PrestoJdbcWorkflowRepository>()
}
