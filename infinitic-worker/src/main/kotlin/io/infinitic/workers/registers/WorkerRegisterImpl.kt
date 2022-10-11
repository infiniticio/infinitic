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

import io.infinitic.cache.config.Cache
import io.infinitic.common.config.logger
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workers.ServiceFactory
import io.infinitic.common.workers.WorkflowFactory
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.registry.RegisteredService
import io.infinitic.common.workers.registry.RegisteredServiceTag
import io.infinitic.common.workers.registry.RegisteredWorkflow
import io.infinitic.common.workers.registry.RegisteredWorkflowEngine
import io.infinitic.common.workers.registry.RegisteredWorkflowTag
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.storage.config.Storage
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
                    w.tagEngine?.let {
                        registerWorkflowTag(WorkflowName(w.name), it.concurrency, it.storage, it.cache)
                    }
                    w.workflowEngine?.let {
                        registerWorkflowEngine(WorkflowName(w.name), it.concurrency, it.storage, it.cache)
                    }
                }

                else -> registerWorkflow(
                    w.name,
                    w.concurrency,
                    w.retry,
                    { w.getInstance() },
                    w.workflowEngine,
                    w.tagEngine
                )
            }
        }

        for (s in workerConfig.services) {
            logger.info { "Service ${s.name}:" }

            when (s.`class`) {
                null -> s.tagEngine?.let {
                    registerTaskTag(ServiceName(s.name), it.concurrency, it.storage, it.cache)
                }

                else -> registerService(s.name, s.concurrency, s.retry, { s.getInstance() }, s.tagEngine)
            }
        }
    }

    /**
     * Register task
     */
    override fun registerService(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: ServiceFactory,
        tagEngine: TaskTag?
    ) {
        logger.info {
            "* task executor".padEnd(25) + ": (instances: $concurrency, class:${factory()::class.java.name})"
        }

        val serviceName = ServiceName(name)
        registry.services[serviceName] = RegisteredService(concurrency, retry, factory)

        when (tagEngine) {
            null -> registerTaskTag(serviceName, concurrency, workerConfig.storage, workerConfig.cache)
            else -> registerTaskTag(serviceName, tagEngine.concurrency, tagEngine.storage, tagEngine.cache)
        }
    }

    /**
     * Register workflow
     */
    override fun registerWorkflow(
        name: String,
        concurrency: Int,
        retry: RetryPolicy?,
        factory: WorkflowFactory,
        engine: WorkflowEngine?,
        tagEngine: WorkflowTag?
    ) {
        logger.info {
            "* workflow executor".padEnd(25) + ": (instances: $concurrency, class:${factory()::class.java.name})"
        }

        val workflowName = WorkflowName(name)
        registry.workflows[workflowName] = RegisteredWorkflow(concurrency, retry, factory)

        when (tagEngine) {
            null -> registerWorkflowTag(workflowName, concurrency, workerConfig.storage, workerConfig.cache)
            else -> registerWorkflowTag(
                workflowName,
                tagEngine.concurrency,
                tagEngine.storage,
                tagEngine.cache
            )
        }

        when (engine) {
            null -> registerWorkflowEngine(
                workflowName,
                concurrency,
                workerConfig.storage,
                workerConfig.cache
            )

            else -> registerWorkflowEngine(workflowName, engine.concurrency, engine.storage, engine.cache)
        }
    }

    private fun registerWorkflowEngine(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: Storage?,
        cache: Cache?
    ) {
        val c = cache ?: workerConfig.cache
        val s = storage ?: workerConfig.storage

        logger.info {
            "* workflow engine".padEnd(25) + ": (storage: ${s.type}, cache: ${c.type}, instances: $concurrency)"
        }

        registry.workflowEngines[workflowName] = RegisteredWorkflowEngine(
            concurrency,
            BinaryWorkflowStateStorage(
                CachedKeyValueStorage(c.keyValue, s.keyValue)
            )
        )
    }

    private fun registerTaskTag(
        serviceName: ServiceName,
        concurrency: Int,
        storage: Storage?,
        cache: Cache?
    ) {
        val c = cache ?: workerConfig.cache
        val s = storage ?: workerConfig.storage

        logger.info {
            "* task tag ".padEnd(25) + ": (storage: ${s.type}, cache: ${c.type}, instances: $concurrency)"
        }

        registry.serviceTags[serviceName] = RegisteredServiceTag(
            concurrency,
            BinaryTaskTagStorage(
                CachedKeyValueStorage(c.keyValue, s.keyValue),
                CachedKeySetStorage(c.keySet, s.keySet)
            )
        )
    }

    private fun registerWorkflowTag(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: Storage?,
        cache: Cache?
    ) {
        val c = cache ?: workerConfig.cache
        val s = storage ?: workerConfig.storage

        logger.info {
            "* workflow tag ".padEnd(25) + ": (storage: ${s.type}, cache: ${c.type}, instances: $concurrency)"
        }

        registry.workflowTags[workflowName] = RegisteredWorkflowTag(
            concurrency,
            BinaryWorkflowTagStorage(
                CachedKeyValueStorage(c.keyValue, s.keyValue),
                CachedKeySetStorage(c.keySet, s.keySet)
            )
        )
    }
}
