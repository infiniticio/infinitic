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

import io.infinitic.common.storage.keySet.CachedKeySetStorage
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.config.WorkerConfig
import io.infinitic.config.cache.getKeySetCache
import io.infinitic.config.cache.getKeyValueCache
import io.infinitic.config.loaders.loadConfigFromFile
import io.infinitic.config.loaders.loadConfigFromResource
import io.infinitic.config.storage.getKeySetStorage
import io.infinitic.config.storage.getKeyValueStorage
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
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
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.PulsarClient
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticWorker private constructor(
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val workerConfig: WorkerConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /**
         * Create InfiniticWorker from a custom PulsarClient and a WorkerConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, workerConfig: WorkerConfig) = InfiniticWorker(pulsarClient, workerConfig)

        /**
         * Create InfiniticWorker from a WorkerConfig
         */
        @JvmStatic
        fun fromConfig(workerConfig: WorkerConfig): InfiniticWorker {
            // build Pulsar client from config
            val pulsarClient: PulsarClient = PulsarClient
                .builder()
                .serviceUrl(workerConfig.pulsar.serviceUrl)
                .build()

            return InfiniticWorker(pulsarClient, workerConfig)
        }

        /**
         * Create InfiniticWorker from WorkerConfig resources
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /**
         * Create InfiniticWorker from WorkerConfig files
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    /**
     * Close worker
     */
    fun close() = pulsarClient.close()

    /**
     * Start worker
     */
    fun start() {
        val threadPool = Executors.newCachedThreadPool()
        val scope = CoroutineScope(threadPool.asCoroutineDispatcher())

        scope.launch {
            try {
                coroutineScope { start(pulsarClient, workerConfig) }
            } finally {
                // must close Pulsar client to avoid other key-shared subscriptions to be blocked
                close()
                // closing the worker is important for devops
                threadPool.shutdown()
            }
        }
    }

    private fun CoroutineScope.start(
        pulsarClient: PulsarClient,
        config: WorkerConfig
    ) = launch {

        val workerName = getProducerName(pulsarClient, config.name)
        val tenant = config.pulsar.tenant
        val namespace = config.pulsar.namespace
        val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
        val pulsarOutput = PulsarOutput.from(pulsarClient, tenant, namespace, workerName)

        val taskExecutorRegister = TaskExecutorRegisterImpl()

        val clientFactory = { InfiniticClient(pulsarClient, tenant, namespace) }

        for (workflow in config.workflows) {
            val workflowName = WorkflowName(workflow.name)
            println("Workflow $workflowName:")

            // starting task executors running workflows tasks
            workflow.`class`?.let {
                println("- workflow executor".padEnd(25) + ": (instances: ${workflow.concurrency})")
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
                println(
                    "- tag engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"

                )
                startPulsarWorkflowTagEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    BinaryWorkflowTagStorage(
                        CachedKeyValueStorage(
                            it.stateCache!!.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        ),
                        CachedKeySetStorage(
                            it.stateCache!!.getKeySetCache(workerConfig),
                            it.stateStorage!!.getKeySetStorage(workerConfig)
                        )
                    ),
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }

            // starting engines managing workflowTasks
            workflow.taskEngine?.let {
                println(
                    "- workflow task engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                )
                startPulsarTaskEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    BinaryTaskStateStorage(
                        CachedKeyValueStorage(
                            it.stateCache!!.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        )
                    ),
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }

            // starting engines managing workflows
            workflow.workflowEngine?.let {
                println(
                    "- workflow engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"

                )
                startPulsarWorkflowEngines(
                    workflowName,
                    workerName,
                    it.concurrency,
                    BinaryWorkflowStateStorage(
                        CachedKeyValueStorage(
                            it.stateCache!!.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        )
                    ),
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

            println()
        }

        for (task in config.tasks) {
            val taskName = TaskName(task.name)
            println("Task $taskName:")

            // starting task executors running tasks
            task.`class`?.let {
                println("- task executor".padEnd(25) + ": (instances: ${task.concurrency})")
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
                println(
                    "- tag engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"

                )
                startPulsarTaskTagEngines(
                    taskName,
                    workerName,
                    it.concurrency,
                    BinaryTaskTagStorage(
                        CachedKeyValueStorage(
                            it.stateCache!!.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        ),
                        CachedKeySetStorage(
                            it.stateCache!!.getKeySetCache(workerConfig),
                            it.stateStorage!!.getKeySetStorage(workerConfig)
                        )
                    ),
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }

            // starting engines managing tasks
            task.taskEngine?.let {
                println(
                    "- task engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCache}" +
                        ", instances: ${it.concurrency})"
                )
                startPulsarTaskEngines(
                    taskName,
                    workerName,
                    it.concurrency,
                    BinaryTaskStateStorage(
                        CachedKeyValueStorage(
                            it.stateCache!!.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        )
                    ),
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
                println(
                    "- metrics engine".padEnd(25) + ": (" +
                        "storage: ${it.stateStorage}" +
                        ", cache: ${it.stateCacheOrDefault})"
                )
                startPulsarMetricsPerNameEngines(
                    taskName,
                    workerName,
                    pulsarConsumerFactory,
                    BinaryMetricsPerNameStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        )
                    ),
                    pulsarOutput
                )

                startPulsarMetricsGlobalEngine(
                    workerName,
                    pulsarConsumerFactory,
                    BinaryMetricsGlobalStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(workerConfig),
                            it.stateStorage!!.getKeyValueStorage(workerConfig)
                        )
                    )
                )
            }
            println()
        }

        println("Worker \"$workerName\" ready")
    }
}
