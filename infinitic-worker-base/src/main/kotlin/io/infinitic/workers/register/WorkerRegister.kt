package io.infinitic.workers.register

import io.infinitic.common.workers.TaskFactory
import io.infinitic.common.workers.WorkflowFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag

interface WorkerRegister {

    val registry: WorkerRegistry

    /**
     * Register task
     */
    fun registerTask(
        name: String,
        concurrency: Int,
        factory: TaskFactory,
        tag: TaskTag? = null
    )

    /**
     * Register workflow
     */
    fun registerWorkflow(
        name: String,
        concurrency: Int,
        factory: WorkflowFactory,
        tag: WorkflowTag? = null,
        engine: WorkflowEngine? = null
    )
}
