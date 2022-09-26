package io.infinitic.workers.registers

import io.infinitic.cache.StateCache
import io.infinitic.common.config.logger
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workers.TaskFactory
import io.infinitic.common.workers.WorkflowFactory
import io.infinitic.common.workers.registry.RegisteredTask
import io.infinitic.common.workers.registry.RegisteredTaskTag
import io.infinitic.common.workers.registry.RegisteredWorkflow
import io.infinitic.common.workers.registry.RegisteredWorkflowEngine
import io.infinitic.common.workers.registry.RegisteredWorkflowTag
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.storage.StateStorage
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.tasks.tag.storage.BinaryTaskTagStorage
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.register.WorkerRegister
import io.infinitic.workers.storage.CachedKeySetStorage
import io.infinitic.workers.storage.CachedKeyValueStorage
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tag.config.WorkflowTag
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage

class WorkerRegisterImpl(private val workerConfig: WorkerConfig) : WorkerRegister {

    override val registry = WorkerRegistry(workerConfig.name)

    init {
        // register WorkflowTasks
//        workerRegister.registerTask(WorkflowTask::class.java.name) { WorkflowTaskImpl() }

        for (w in workerConfig.workflows) {
            logger.info { "Workflow ${w.name}:" }

            when (w.`class`) {
                null -> {
                    w.workflowTag?.let {
                        registerWorkflowTag(WorkflowName(w.name), it.concurrency, it.stateStorage, it.stateCache)
                    }
                    w.workflowEngine?.let {
                        registerWorkflowEngine(WorkflowName(w.name), it.concurrency, it.stateStorage, it.stateCache)
                    }
                }

                else -> registerWorkflow(w.name, w.concurrency, { w.instance }, w.workflowTag, w.workflowEngine)
            }
        }

        for (t in workerConfig.tasks) {
            logger.info { "Task ${t.name}:" }

            when (t.`class`) {
                null -> t.taskTag?.let {
                    registerTaskTag(TaskName(t.name), it.concurrency, it.stateStorage, it.stateCache)
                }

                else -> registerTask(t.name, t.concurrency, { t.instance }, t.taskTag)
            }
        }
    }

    /**
     * Register task
     */
    override fun registerTask(name: String, concurrency: Int, factory: TaskFactory, tag: TaskTag?) {
        logger.info {
            "* task executor".padEnd(25) + ": (instances: $concurrency, class:${factory()::class.java.name})"
        }

        val taskName = TaskName(name)
        registry.tasks[taskName] = RegisteredTask(concurrency, factory)

        when (tag) {
            null -> registerTaskTag(taskName, concurrency, workerConfig.stateStorage, workerConfig.stateCache)
            else -> registerTaskTag(taskName, tag.concurrency, tag.stateStorage, tag.stateCache)
        }
    }

    /**
     * Register workflow
     */
    override fun registerWorkflow(
        name: String,
        concurrency: Int,
        factory: WorkflowFactory,
        tag: WorkflowTag?,
        engine: WorkflowEngine?
    ) {
        logger.info {
            "* workflow executor".padEnd(25) + ": (instances: $concurrency, class:${factory()::class.java.name})"
        }

        val workflowName = WorkflowName(name)
        registry.workflows[workflowName] = RegisteredWorkflow(concurrency, factory)

        when (tag) {
            null -> registerWorkflowTag(workflowName, concurrency, workerConfig.stateStorage, workerConfig.stateCache)
            else -> registerWorkflowTag(workflowName, tag.concurrency, tag.stateStorage, tag.stateCache)
        }

        when (engine) {
            null -> registerWorkflowEngine(
                workflowName,
                concurrency,
                workerConfig.stateStorage,
                workerConfig.stateCache
            )

            else -> registerWorkflowEngine(workflowName, engine.concurrency, engine.stateStorage, engine.stateCache)
        }
    }

    private fun registerWorkflowEngine(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: StateStorage?,
        cache: StateCache?
    ) {
        val c = cache ?: workerConfig.stateCache
        val s = storage ?: workerConfig.stateStorage

        logger.info {
            "* workflow engine".padEnd(25) + ": (storage: $s, cache: $c, instances: $concurrency)"
        }

        registry.workflowEngines[workflowName] = RegisteredWorkflowEngine(
            concurrency,
            BinaryWorkflowStateStorage(
                CachedKeyValueStorage(c.keyValue(workerConfig), s.keyValue(workerConfig))
            )
        )
    }

    private fun registerTaskTag(
        taskName: TaskName,
        concurrency: Int,
        storage: StateStorage?,
        cache: StateCache?
    ) {
        val c = cache ?: workerConfig.stateCache
        val s = storage ?: workerConfig.stateStorage

        logger.info {
            "* task tag ".padEnd(25) + ": (storage: $s, cache: $c, instances: $concurrency)"
        }

        registry.taskTags[taskName] = RegisteredTaskTag(
            concurrency,
            BinaryTaskTagStorage(
                CachedKeyValueStorage(c.keyValue(workerConfig), s.keyValue(workerConfig)),
                CachedKeySetStorage(c.keySet(workerConfig), s.keySet(workerConfig))
            )
        )
    }

    private fun registerWorkflowTag(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: StateStorage?,
        cache: StateCache?
    ) {
        val c = cache ?: workerConfig.stateCache
        val s = storage ?: workerConfig.stateStorage

        logger.info {
            "* workflow tag ".padEnd(25) + ": (storage: $s, cache: $c, instances: $concurrency)"
        }

        registry.workflowTags[workflowName] = RegisteredWorkflowTag(
            concurrency,
            BinaryWorkflowTagStorage(
                CachedKeyValueStorage(c.keyValue(workerConfig), s.keyValue(workerConfig)),
                CachedKeySetStorage(c.keySet(workerConfig), s.keySet(workerConfig))
            )
        )
    }
}
