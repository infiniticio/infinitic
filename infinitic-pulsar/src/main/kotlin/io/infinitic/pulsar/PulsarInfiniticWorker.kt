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

package io.infinitic.pulsar

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.storage.keySet.CachedKeySetStorage
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.config.WorkerConfig
import io.infinitic.config.data.Transport
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.pulsar.workers.startPulsarMetricsGlobalEngine
import io.infinitic.pulsar.workers.startPulsarMetricsPerNameEngines
import io.infinitic.pulsar.workers.startPulsarTaskDelayEngines
import io.infinitic.pulsar.workers.startPulsarTaskEngines
import io.infinitic.pulsar.workers.startPulsarTaskExecutors
import io.infinitic.pulsar.workers.startPulsarTaskTagEngines
import io.infinitic.pulsar.workers.startPulsarWorkflowDelayEngines
import io.infinitic.pulsar.workers.startPulsarWorkflowEngines
import io.infinitic.pulsar.workers.startPulsarWorkflowTagEngines
import io.infinitic.tags.tasks.storage.BinaryTaskTagStorage
import io.infinitic.tags.tasks.storage.TaskTagStorage
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PulsarInfiniticWorker private constructor(
    val pulsarClient: PulsarClient,
    val workerConfig: WorkerConfig
) : Closeable {
    private val logger = KotlinLogging.logger {}

    private val taskStorages = mutableMapOf<TaskName, TaskStateStorage>()
    private val taskTagStorages = mutableMapOf<TaskName, TaskTagStorage>()
    private val workflowStorages = mutableMapOf<WorkflowName, WorkflowStateStorage>()
    private val workflowTagStorages = mutableMapOf<WorkflowName, WorkflowTagStorage>()
    private val workflowTaskStorages = mutableMapOf<WorkflowName, TaskStateStorage>()
    private val perNameStorages = mutableMapOf<TaskName, MetricsPerNameStateStorage>()
    private val globalStorages = mutableMapOf<TaskName, MetricsGlobalStateStorage>()

    private val runningThreadPool = Executors.newCachedThreadPool()
    val runningScope = CoroutineScope(runningThreadPool.asCoroutineDispatcher() + Job())

    companion object {
        /**
         * Create InfiniticWorker from a custom PulsarClient and a WorkerConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, workerConfig: WorkerConfig) =
            PulsarInfiniticWorker(pulsarClient, workerConfig)

        /**
         * Create InfiniticWorker from a WorkerConfig
         */
        @JvmStatic
        fun fromConfig(workerConfig: WorkerConfig): PulsarInfiniticWorker =
            PulsarInfiniticWorker(workerConfig.pulsar.client, workerConfig)

        /**
         * Create InfiniticWorker from file in resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(WorkerConfig.fromResource(*resources))

        /**
         * Create InfiniticWorker from file in system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(WorkerConfig.fromFile(*files))
    }

    /**
     * Close worker
     */
    override fun close() {
        runningScope.cancel()
        runningThreadPool.shutdown()

        pulsarClient.close()
    }

    /**
     * Start worker
     */
    fun start() {
        when (workerConfig.transport) {
            Transport.inMemory -> {
                logger.info { "Infinitic Worker is closing due to `inMemory` transport setting" }
                close()
            }
            Transport.pulsar -> {
                runningScope.future { start(pulsarClient, workerConfig) }.join()
            }
        }
    }

    @TestOnly fun taskStorage(name: String) = taskStorages[TaskName(name)]
    @TestOnly fun taskTagStorage(name: String) = taskTagStorages[TaskName(name)]
    @TestOnly fun workflowStorage(name: String) = workflowStorages[WorkflowName(name)]
    @TestOnly fun workflowTagStorage(name: String) = workflowTagStorages[WorkflowName(name)]

    @TestOnly fun storageFlush() {
        taskStorages.forEach { it.value.flush() }
        taskTagStorages.forEach { it.value.flush() }
        workflowStorages.forEach { it.value.flush() }
        workflowTagStorages.forEach { it.value.flush() }
        workflowTaskStorages.forEach { it.value.flush() }
        perNameStorages.forEach { it.value.flush() }
        globalStorages.forEach { it.value.flush() }
    }

    private fun CoroutineScope.start(
        pulsarClient: PulsarClient,
        config: WorkerConfig
    ) = launch {
        val workerName = getProducerName(pulsarClient, config.pulsar.tenant, config.pulsar.namespace, config.name)
        val tenant = config.pulsar.tenant
        val namespace = config.pulsar.namespace
        val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
        val pulsarOutput = PulsarOutput.from(pulsarClient, tenant, namespace, workerName)

        val taskExecutorRegister = TaskExecutorRegisterImpl()

        val clientFactory = { PulsarInfiniticClient(pulsarClient, tenant, namespace) }

        for (workflow in config.workflows) {
            val workflowName = WorkflowName(workflow.name)
            logger.info { "Workflow $workflowName:" }

            // starting task executors running workflows tasks
            workflow.`class`?.let {
                logger.info {
                    "- workflow executor".padEnd(25) +
                        ": (instances: ${workflow.concurrency}) ${workflow.instance::class.java.name}"
                }
                taskExecutorRegister.registerWorkflow(workflow.name) { workflow.instance }
                startPulsarTaskExecutors(
                    workflowName,
                    workflow.concurrency,
                    workerName,
                    taskExecutorRegister,
                    pulsarConsumerFactory,
                    pulsarOutput,
                    clientFactory
                )
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

                startPulsarWorkflowTagEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    storage,
                    pulsarConsumerFactory,
                    pulsarOutput
                )
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
                        it.stateStorage!!.keyValue(workerConfig),
                        cachePersistenceAfterDeletion = MillisDuration(workerConfig.stateCachePersistenceAfterDeletion)
                    )
                )

                workflowTaskStorages[workflowName] = storage

                startPulsarTaskEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    storage,
                    pulsarConsumerFactory,
                    pulsarOutput
                )

                startPulsarTaskDelayEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    pulsarConsumerFactory,
                    pulsarOutput
                )
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
                        it.stateStorage!!.keyValue(workerConfig),
                        cachePersistenceAfterDeletion = MillisDuration(workerConfig.stateCachePersistenceAfterDeletion)
                    )
                )

                workflowStorages[workflowName] = storage

                startPulsarWorkflowEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    storage,
                    pulsarConsumerFactory,
                    pulsarOutput
                )

                startPulsarWorkflowDelayEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }
        }

        for (task in config.tasks) {
            val taskName = TaskName(task.name)
            logger.info { "Task $taskName:" }

            // starting task executors running tasks
            task.`class`?.let {
                logger.info {
                    "- task executor".padEnd(25) +
                        ": (instances: ${task.concurrency}) ${task.instance::class.java.name}"
                }
                taskExecutorRegister.registerTask(task.name) { task.instance }
                startPulsarTaskExecutors(
                    taskName,
                    task.concurrency,
                    workerName,
                    taskExecutorRegister,
                    pulsarConsumerFactory,
                    pulsarOutput,
                    clientFactory
                )
            }

            // starting engines managing tags of tasks
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

                startPulsarTaskTagEngines(
                    taskName,
                    workerName,
                    it.concurrency,
                    storage,
                    pulsarConsumerFactory,
                    pulsarOutput
                )
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

                startPulsarTaskEngines(
                    taskName,
                    workerName,
                    it.concurrency,
                    storage,
                    pulsarConsumerFactory,
                    pulsarOutput
                )

                startPulsarTaskDelayEngines(
                    taskName,
                    workerName,
                    it.concurrency,
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }

            task.metrics?.let {
                logger.info {
                    "- metrics engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCacheOrDefault})"
                }

                val perNameStorage = BinaryMetricsPerNameStateStorage(
                    CachedKeyValueStorage(
                        it.stateCacheOrDefault.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                perNameStorages[taskName] = perNameStorage

                startPulsarMetricsPerNameEngines(
                    taskName,
                    workerName,
                    pulsarConsumerFactory,
                    perNameStorage,
                    pulsarOutput
                )

                val globalStorage = BinaryMetricsGlobalStateStorage(
                    CachedKeyValueStorage(
                        it.stateCacheOrDefault.keyValue(workerConfig),
                        it.stateStorage!!.keyValue(workerConfig)
                    )
                )
                globalStorages[taskName] = globalStorage

                startPulsarMetricsGlobalEngine(
                    workerName,
                    pulsarConsumerFactory,
                    globalStorage
                )
            }
        }
        logger.info { "Worker \"$workerName\" ready" }
    }
}
