/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

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

                else -> registerWorkflow(w.name, w.concurrency, { w.instance }, w.workflowEngine, w.workflowTag)
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
    override fun registerTask(
        name: String,
        concurrency: Int,
        factory: TaskFactory,
        tagEngine: TaskTag?
    ) {
        logger.info {
            "* task executor".padEnd(25) + ": (instances: $concurrency, class:${factory()::class.java.name})"
        }

        val taskName = TaskName(name)
        registry.tasks[taskName] = RegisteredTask(concurrency, factory)

        when (tagEngine) {
            null -> registerTaskTag(taskName, concurrency, workerConfig.stateStorage, workerConfig.stateCache)
            else -> registerTaskTag(taskName, tagEngine.concurrency, tagEngine.stateStorage, tagEngine.stateCache)
        }
    }

    /**
     * Register workflow
     */
    override fun registerWorkflow(
        name: String,
        concurrency: Int,
        factory: WorkflowFactory,
        engine: WorkflowEngine?,
        tagEngine: WorkflowTag?
    ) {
        logger.info {
            "* workflow executor".padEnd(25) + ": (instances: $concurrency, class:${factory()::class.java.name})"
        }

        val workflowName = WorkflowName(name)
        registry.workflows[workflowName] = RegisteredWorkflow(concurrency, factory)

        when (tagEngine) {
            null -> registerWorkflowTag(workflowName, concurrency, workerConfig.stateStorage, workerConfig.stateCache)
            else -> registerWorkflowTag(
                workflowName,
                tagEngine.concurrency,
                tagEngine.stateStorage,
                tagEngine.stateCache
            )
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
