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
import io.infinitic.config.WorkerConfig
import io.infinitic.config.cache.getKeySetCache
import io.infinitic.config.cache.getKeyValueCache
import io.infinitic.config.data.Mode
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
import io.infinitic.pulsar.workers.startPulsarTagEngines
import io.infinitic.pulsar.workers.startPulsarTaskEngines
import io.infinitic.pulsar.workers.startPulsarTaskExecutors
import io.infinitic.pulsar.workers.startPulsarWorkflowEngines
import io.infinitic.tags.engine.storage.BinaryTagStateStorage
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.apache.pulsar.client.api.PulsarClient
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticWorker(
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val config: WorkerConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /*
        Create InfiniticWorker from a WorkerConfig
        */
        @JvmStatic
        fun fromConfig(config: WorkerConfig): InfiniticWorker {
            // build Pulsar client from config
            val pulsarClient: PulsarClient = PulsarClient
                .builder()
                .serviceUrl(config.pulsar.serviceUrl)
                .build()

            return InfiniticWorker(pulsarClient, config)
        }

        /*
        Create InfiniticWorker from a WorkerConfig loaded from a resource
        */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
        Create InfiniticWorker from a WorkerConfig loaded from a file
        */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    /*
    Close workers
    */
    fun close() = pulsarClient.close()

    /*
    Start workers
    */
    fun start() {
        val threadPool = Executors.newCachedThreadPool()
        val scope = CoroutineScope(threadPool.asCoroutineDispatcher())

        with(scope) {
            logger.info("InfiniticWorker - starting with config {}", config)

            val workerName = getPulsarName(pulsarClient, config.name)
            val tenant = config.pulsar.tenant
            val namespace = config.pulsar.namespace
            val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
            val pulsarOutputs = PulsarOutput.from(pulsarClient, tenant, namespace, workerName)

            startTagEngineWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

            startTaskEngineWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

            startWorkflowEngineWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

            startMetricsWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

            startTaskExecutorWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

            println("Worker \"$workerName\" ready")
        }
    }

    private fun CoroutineScope.startTagEngineWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        config.tagEngine?.let {
            if (it.modeOrDefault == Mode.worker) {
                println(
                    "Tag engine".padEnd(25) + ": ${it.concurrencyOrDefault} instances " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                startPulsarTagEngines(
                    consumerName,
                    it.concurrencyOrDefault,
                    BinaryTagStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(config),
                            it.stateStorage!!.getKeyValueStorage(config)
                        ),
                        CachedKeySetStorage(
                            it.stateCacheOrDefault.getKeySetCache(config),
                            it.stateStorage!!.getKeySetStorage(config)
                        )
                    ),
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }
        }
    }

    private fun CoroutineScope.startTaskEngineWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        config.taskEngine?.let {
            if (it.modeOrDefault == Mode.worker) {
                println(
                    "Task engine".padEnd(25) + ": ${it.concurrency} instances " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                startPulsarTaskEngines(
                    consumerName,
                    it.concurrency,
                    BinaryTaskStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(config),
                            it.stateStorage!!.getKeyValueStorage(config)
                        )
                    ),
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }
        }
    }

    private fun CoroutineScope.startWorkflowEngineWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        config.workflowEngine?.let {
            if (it.modeOrDefault == Mode.worker) {
                println(
                    "Workflow engine".padEnd(25) + ": ${it.concurrency} instances " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                startPulsarWorkflowEngines(
                    consumerName,
                    it.concurrency,
                    BinaryWorkflowStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(config),
                            it.stateStorage!!.getKeyValueStorage(config)
                        )
                    ),
                    pulsarConsumerFactory,
                    pulsarOutput
                )
            }
        }
    }

    private fun CoroutineScope.startMetricsWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        config.metrics?.let {
            if (it.modeOrDefault == Mode.worker) {
                println(
                    "Metrics engine".padEnd(25) + ": ${it.concurrency} instances " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                logger.info("InfiniticWorker - starting {} metrics per name engines", it.concurrency)
                startPulsarMetricsPerNameEngines(
                    it.concurrency,
                    consumerName,
                    pulsarConsumerFactory,
                    BinaryMetricsPerNameStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(config),
                            it.stateStorage!!.getKeyValueStorage(config)
                        )
                    ),
                    pulsarOutput.sendToMetricsGlobal
                )
                logger.info("InfiniticWorker - starting metrics global engine")
                startPulsarMetricsGlobalEngine(
                    pulsarConsumerFactory.newMetricsGlobalEngineConsumer(consumerName),
                    BinaryMetricsGlobalStateStorage(
                        CachedKeyValueStorage(
                            it.stateCacheOrDefault.getKeyValueCache(config),
                            it.stateStorage!!.getKeyValueStorage(config)
                        )
                    ),
                )
            }
        }
    }

    private fun CoroutineScope.startTaskExecutorWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        val taskExecutorRegister = TaskExecutorRegisterImpl()

        for (workflow in config.workflows) {
            if (workflow.modeOrDefault == Mode.worker) {
                taskExecutorRegister.registerWorkflow(workflow.name) { workflow.instance }
                println("Workflow executor".padEnd(25) + ": ${workflow.concurrency} instances (${workflow.name})")
                logger.info("InfiniticWorker - starting {} workflow executors for {}", workflow.concurrency, workflow.name)
                startPulsarTaskExecutors(
                    workflow.concurrency,
                    taskExecutorRegister,
                    pulsarConsumerFactory.newWorkflowExecutorConsumer(consumerName, workflow.name),
                    pulsarOutput.sendEventsToTaskEngine
                )
            }
        }

        for (task in config.tasks) {
            if (task.modeOrDefault == Mode.worker) {
                taskExecutorRegister.registerTask(task.name) { task.instance }
                println("Task executor".padEnd(25) + ": ${task.concurrency} instances (${task.name})")
                logger.info("InfiniticWorker - starting {} task executor for {}", task.concurrency, task.name)
                startPulsarTaskExecutors(
                    task.concurrency,
                    taskExecutorRegister,
                    pulsarConsumerFactory.newTaskExecutorConsumer(consumerName, task.name),
                    pulsarOutput.sendEventsToTaskEngine
                )
            }
        }
    }
}
