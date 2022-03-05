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

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.storage.keySet.CachedKeySetStorage
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engines.storage.TaskStateStorage
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.workers.WorkerStarter
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.tags.tasks.storage.BinaryTaskTagStorage
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.register.WorkerRegisterImpl
import io.infinitic.tasks.metrics.storage.BinaryTaskMetricsStateStorage
import io.infinitic.worker.config.WorkerConfig
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.workflowTask.WorkflowTaskImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import mu.KotlinLogging
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class InfiniticWorker(open val workerConfig: WorkerConfig) : Closeable {
    protected val logger = KotlinLogging.logger {}

    protected val workerRegister = WorkerRegisterImpl()

    protected val taskStateStorages = mutableMapOf<TaskName, TaskStateStorage>()
    protected val taskTagStorages = mutableMapOf<TaskName, TaskTagStorage>()
    protected val taskMetricsStateStorages = mutableMapOf<TaskName, TaskMetricsStateStorage>()
    protected val workflowStateStorages = mutableMapOf<WorkflowName, WorkflowStateStorage>()
    protected val workflowTagStorages = mutableMapOf<WorkflowName, WorkflowTagStorage>()
    protected val workflowTaskStateStorages = mutableMapOf<WorkflowName, TaskStateStorage>()
    protected val workflowTaskMetricsStateStorages = mutableMapOf<WorkflowName, TaskMetricsStateStorage>()

    protected abstract val workerStarter: WorkerStarter
    protected abstract val clientFactory: ClientFactory
    protected abstract val name: String

    /**
     * Start worker synchronously
     */
    abstract fun start()

    /**
     * Start worker asynchronously
     */
    abstract fun startAsync(): CompletableFuture<Unit>

    /**
     * Flush all storages (test-only)
     */
    @TestOnly
    fun storageFlush() {
        taskStateStorages.forEach { it.value.flush() }
        taskTagStorages.forEach { it.value.flush() }
        workflowStateStorages.forEach { it.value.flush() }
        workflowTagStorages.forEach { it.value.flush() }
        workflowTaskStateStorages.forEach { it.value.flush() }
        taskMetricsStateStorages.forEach { it.value.flush() }
    }

    /**
     * Start worker synchronously on provided scope
     */
    fun CoroutineScope.start(): Unit = startAsync().join()

    /**
     * Start worker asynchronously on provided scope
     */
    fun CoroutineScope.startAsync() = future {
        // register WorkflowTasks
        workerRegister.registerTask(WorkflowTask::class.java.name) { WorkflowTaskImpl() }

        for (workflow in workerConfig.workflows) {
            val workflowName = WorkflowName(workflow.name)
            logger.info { "Workflow $workflowName:" }

            // starting task executors running workflows tasks
            workflow.`class`?.let {
                logger.info {
                    "* workflow executor".padEnd(25) +
                        ": (instances: ${workflow.concurrency}) ${workflow.instance::class.java.name}"
                }
                workerRegister.registerWorkflow(workflow.name) { workflow.instance }

                with(workerStarter) {
                    startWorkflowTaskExecutor(workflowName, workflow.concurrency, workerRegister, clientFactory)
                }
            }

            workflow.tagEngine?.let {
                // starting engines managing tags of workflows
                logger.info {
                    "* workflow tag ".padEnd(25) + ": (" +
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

                with(workerStarter) {
                    startWorkflowTag(workflowName, storage, it.concurrency)
                }
            }

            // starting engines managing workflows
            workflow.workflowEngine?.let {
                logger.info {
                    "* workflow engine".padEnd(25) + ": (" +
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
                workflowStateStorages[workflowName] = storage

                with(workerStarter) {
                    startWorkflowEngine(workflowName, storage, it.concurrency)
                    startWorkflowDelay(workflowName, it.concurrency)
                }
            }

            // starting engines managing workflowTasks
            workflow.taskEngine?.let {
                logger.info {
                    "* workflow task engine".padEnd(25) + ": (" +
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
                workflowTaskStateStorages[workflowName] = storage

                with(workerStarter) {
                    startWorkflowTaskEngine(workflowName, storage, it.concurrency)
                    startWorkflowTaskDelay(workflowName, it.concurrency)
                }
            }

            workflow.metrics?.let {
                logger.info {
                    "* workflow task metrics".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCacheOrDefault})"
                }

                val workflowTaskMetricsStateStorage = BinaryTaskMetricsStateStorage(
                    CachedKeyValueStorage(
                        it.stateCacheOrDefault.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                workflowTaskMetricsStateStorages[workflowName] = workflowTaskMetricsStateStorage

                with(workerStarter) {
                    startWorkflowTaskMetrics(workflowName, workflowTaskMetricsStateStorage, workflow.concurrency)
                }
            }
        }

        for (task in workerConfig.tasks) {
            val taskName = TaskName(task.name)
            logger.info { "Task $taskName:" }

            // starting task executors running tasks
            task.`class`?.let {
                logger.info {
                    "* task executor".padEnd(25) +
                        ": (instances: ${task.concurrency}) ${task.instance::class.java.name}"
                }
                workerRegister.registerTask(task.name) { task.instance }

                with(workerStarter) {
                    startTaskExecutor(taskName, task.concurrency, workerRegister, clientFactory)
                }
            }

            // starting engines managing tags of taskws
            task.tagEngine?.let {
                logger.info {
                    "* tag engine".padEnd(25) + ": (" +
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

                with(workerStarter) {
                    startTaskTag(taskName, storage, task.concurrency)
                }
            }

            // starting engines managing tasks
            task.taskEngine?.let {
                logger.info {
                    "* task engine".padEnd(25) + ": (" +
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
                taskStateStorages[taskName] = storage

                with(workerStarter) {
                    startTaskEngine(taskName, storage, task.concurrency)
                    startTaskDelay(taskName, task.concurrency)
                }
            }

            task.metrics?.let {
                logger.info {
                    "* task metrics".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCacheOrDefault})"
                }

                val taskMetricsStateStorage = BinaryTaskMetricsStateStorage(
                    CachedKeyValueStorage(
                        it.stateCacheOrDefault.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                taskMetricsStateStorages[taskName] = taskMetricsStateStorage

                with(workerStarter) {
                    startTaskMetrics(taskName, taskMetricsStateStorage, task.concurrency)
                }

                logger.info {
                    "* global metrics".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCacheOrDefault})"
                }
            }
        }
        logger.info { "Worker \"$name\" ready" }

        // wait for completion of all launched coroutines
        coroutineContext.job.join()
    }
}
