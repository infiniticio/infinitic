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

package io.infinitic.worker

import io.infinitic.common.data.Name
import io.infinitic.common.metrics.global.storage.MetricsGlobalStateStorage
import io.infinitic.common.storage.keySet.CachedKeySetStorage
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.storage.TaskStateStorage
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.storage.BinaryTaskMetricsStateStorage
import io.infinitic.tags.tasks.storage.BinaryTaskTagStorage
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.worker.config.WorkerConfig
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.workflowTask.WorkflowTaskImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import mu.KotlinLogging
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class InfiniticWorker(open val workerConfig: WorkerConfig) : Closeable {
    protected val logger = KotlinLogging.logger {}

    private val runningThreadPool = Executors.newCachedThreadPool()
    val runningScope = CoroutineScope(runningThreadPool.asCoroutineDispatcher() + Job())

    protected val taskExecutorRegister = TaskExecutorRegisterImpl()

    protected val taskStorages = mutableMapOf<TaskName, TaskStateStorage>()
    protected val taskTagStorages = mutableMapOf<TaskName, TaskTagStorage>()
    protected val workflowStorages = mutableMapOf<WorkflowName, WorkflowStateStorage>()
    protected val workflowTagStorages = mutableMapOf<WorkflowName, WorkflowTagStorage>()
    protected val workflowTaskStorages = mutableMapOf<WorkflowName, TaskStateStorage>()
    protected val perNameStorages = mutableMapOf<TaskName, TaskMetricsStateStorage>()
    protected val globalStorages = mutableMapOf<TaskName, MetricsGlobalStateStorage>()

    abstract val name: String

    protected abstract fun startTaskExecutors(name: Name, concurrency: Int)

    protected abstract fun startWorkflowTagEngines(workflowName: WorkflowName, concurrency: Int, storage: WorkflowTagStorage)
    protected abstract fun startTaskEngines(workflowName: WorkflowName, concurrency: Int, storage: TaskStateStorage)
    protected abstract fun startTaskDelayEngines(workflowName: WorkflowName, concurrency: Int)
    protected abstract fun startWorkflowEngines(workflowName: WorkflowName, concurrency: Int, storage: WorkflowStateStorage)
    protected abstract fun startWorkflowDelayEngines(workflowName: WorkflowName, concurrency: Int)

    protected abstract fun startTaskTagEngines(taskName: TaskName, concurrency: Int, storage: TaskTagStorage)
    protected abstract fun startTaskEngines(taskName: TaskName, concurrency: Int, storage: TaskStateStorage)
    protected abstract fun startTaskDelayEngines(taskName: TaskName, concurrency: Int)
    protected abstract fun startMetricsPerNameEngines(taskName: TaskName, storage: TaskMetricsStateStorage)

    protected abstract fun startMetricsGlobalEngine(storage: MetricsGlobalStateStorage)

    /**
     * Close worker
     */
    override fun close() {
        runningScope.cancel()
        runningThreadPool.shutdown()
    }

    /**
     * Flush all storages (test-only)
     */
    @TestOnly fun storageFlush() {
        taskStorages.forEach { it.value.flush() }
        taskTagStorages.forEach { it.value.flush() }
        workflowStorages.forEach { it.value.flush() }
        workflowTagStorages.forEach { it.value.flush() }
        workflowTaskStorages.forEach { it.value.flush() }
        perNameStorages.forEach { it.value.flush() }
        globalStorages.forEach { it.value.flush() }
    }

    /**
     * Start worker synchronously
     */
    open fun start(): Unit = startAsync().join()

    /**
     * Start worker asynchronously
     */
    open fun startAsync(): CompletableFuture<Unit> {
        // register WorkflowTasks
        taskExecutorRegister.registerTask(WorkflowTask::class.java.name) { WorkflowTaskImpl() }

        for (workflow in workerConfig.workflows) {
            val workflowName = WorkflowName(workflow.name)
            logger.info { "Workflow $workflowName:" }

            // starting task executors running workflows tasks
            workflow.`class`?.let {
                logger.info {
                    "- workflow executor".padEnd(25) +
                        ": (instances: ${workflow.concurrency}) ${workflow.instance::class.java.name}"
                }
                taskExecutorRegister.registerWorkflow(workflow.name) { workflow.instance }

                startTaskExecutors(workflowName, workflow.concurrency)
            }

            workflow.tagEngine?.let {
                // starting engines managing tags of workflows
                logger.info {
                    "- tag engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                }

                val storage = BinaryWorkflowTagStorage(
                    CachedKeyValueStorage(
                        it.stateCache!!.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    ),
                    CachedKeySetStorage(
                        it.stateCache!!.keySet(workerConfig),
                        it.stateStorage!!.keySet(workerConfig)
                    )
                )
                workflowTagStorages[workflowName] = storage

                startWorkflowTagEngines(workflowName, it.concurrency, storage)
            }

            // starting engines managing workflowTasks
            workflow.taskEngine?.let {
                logger.info {
                    "- workflow task engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                }

                val storage = BinaryTaskStateStorage(
                    CachedKeyValueStorage(
                        it.stateCache!!.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                workflowTaskStorages[workflowName] = storage

                startTaskEngines(workflowName, it.concurrency, storage)
                startTaskDelayEngines(workflowName, it.concurrency)
            }

            // starting engines managing workflows
            workflow.workflowEngine?.let {
                logger.info {
                    "- workflow engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                }

                val storage = BinaryWorkflowStateStorage(
                    CachedKeyValueStorage(
                        it.stateCache!!.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                workflowStorages[workflowName] = storage

                startWorkflowEngines(workflowName, it.concurrency, storage)
                startWorkflowDelayEngines(workflowName, it.concurrency)
            }
        }

        for (task in workerConfig.tasks) {
            val taskName = TaskName(task.name)
            logger.info { "Task $taskName:" }

            // starting task executors running tasks
            task.`class`?.let {
                logger.info {
                    "- task executor".padEnd(25) +
                        ": (instances: ${task.concurrency}) ${task.instance::class.java.name}"
                }
                taskExecutorRegister.registerTask(task.name) { task.instance }

                startTaskExecutors(taskName, task.concurrency)
            }

            // starting engines managing tags of taskws
            task.tagEngine?.let {
                logger.info {
                    "- tag engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                }

                val storage = BinaryTaskTagStorage(
                    CachedKeyValueStorage(
                        it.stateCache!!.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    ),
                    CachedKeySetStorage(
                        it.stateCache!!.keySet(workerConfig),
                        it.stateStorage!!.keySet(workerConfig)
                    )
                )
                taskTagStorages[taskName] = storage

                startTaskTagEngines(taskName, it.concurrency, storage)
            }

            // starting engines managing tasks
            task.taskEngine?.let {
                logger.info {
                    "- task engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                }

                val storage = BinaryTaskStateStorage(
                    CachedKeyValueStorage(
                        it.stateCache!!.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                taskStorages[taskName] = storage

                startTaskEngines(taskName, it.concurrency, storage)
                startTaskDelayEngines(taskName, it.concurrency)
            }

            task.metrics?.let {
                logger.info {
                    "- metrics engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCacheOrDefault})"
                }

                val perNameStorage = BinaryTaskMetricsStateStorage(
                    CachedKeyValueStorage(
                        it.stateCacheOrDefault.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                perNameStorages[taskName] = perNameStorage

                startMetricsPerNameEngines(taskName, perNameStorage)

                val globalStorage = BinaryMetricsGlobalStateStorage(
                    CachedKeyValueStorage(
                        it.stateCacheOrDefault.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                globalStorages[taskName] = globalStorage

                startMetricsGlobalEngine(globalStorage)
            }
        }
        logger.info { "Worker \"$name\" ready" }

        // provides a CompletableFuture that waits for completion of all launched coroutines
        return runningScope.future { coroutineContext.job.join() }
    }
}
