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
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutputs
import io.infinitic.pulsar.workers.startPulsarMonitoringGlobalWorker
import io.infinitic.pulsar.workers.startPulsarMonitoringPerNameWorker
import io.infinitic.pulsar.workers.startPulsarTagEngineWorker
import io.infinitic.pulsar.workers.startPulsarTaskEngineWorker
import io.infinitic.pulsar.workers.startPulsarTaskExecutorWorker
import io.infinitic.pulsar.workers.startPulsarWorkflowEngineWorker
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
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
    fun start() = runBlocking {
        logger.info("InfiniticWorker - starting with config {}", config)

        val workerName = getPulsarName(pulsarClient, config.name)
        val tenant = config.pulsar.tenant
        val namespace = config.pulsar.namespace
        val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
        val pulsarOutputs = PulsarOutputs.from(pulsarClient, tenant, namespace, workerName)

        startTagEngineWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

        startTaskEngineWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

        startWorkflowEngineWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

        startMonitoringWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

        startTaskExecutorWorkers(workerName, config, pulsarConsumerFactory, pulsarOutputs)

        println("Worker \"$workerName\" ready")
    }

    private fun CoroutineScope.startTagEngineWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.tagEngine?.let {
            if (it.modeOrDefault == Mode.worker) {
                // storage decorated by the cache
                val keyValueStorage = CachedKeyValueStorage(
                    it.stateCacheOrDefault.getKeyValueCache(config),
                    it.stateStorage!!.getKeyValueStorage(config)
                )
                val keySetStorage = CachedKeySetStorage(
                    it.stateCacheOrDefault.getKeySetCache(config),
                    it.stateStorage!!.getKeySetStorage(config)
                )
                print(
                    "Tag engine".padEnd(25) + ": starting ${it.consumers} instances... " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                repeat(it.consumersOrDefault) { counter ->
                    logger.info("InfiniticWorker - starting tag engine {}", counter)
                    startPulsarTagEngineWorker(
                        counter,
                        pulsarConsumerFactory.newTagEngineConsumer(consumerName, counter),
                        keySetStorage,
                        pulsarOutputs.sendEventsToClient,
                        pulsarOutputs.sendCommandsToTaskEngine,
                        pulsarOutputs.sendCommandsToWorkflowEngine,
                        keyValueStorage,
                    )
                }
                println(" done")
            }
        }
    }

    private fun CoroutineScope.startTaskEngineWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.taskEngine?.let {
            if (it.modeOrDefault == Mode.worker) {
                val keyValueStorage = CachedKeyValueStorage(
                    it.stateCacheOrDefault.getKeyValueCache(config),
                    it.stateStorage!!.getKeyValueStorage(config)
                )
                print(
                    "Task engine".padEnd(25) + ": starting ${it.consumers} instances... " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                repeat(it.consumersOrDefault) { counter ->
                    logger.info("InfiniticWorker - starting task engine {}", counter)
                    startPulsarTaskEngineWorker(
                        counter,
                        pulsarConsumerFactory.newTaskEngineConsumer(consumerName, counter),
                        keyValueStorage,
                        pulsarOutputs.sendEventsToClient,
                        pulsarOutputs.sendEventsToTagEngine,
                        pulsarOutputs.sendEventsToTaskEngine,
                        pulsarOutputs.sendEventsToWorkflowEngine,
                        pulsarOutputs.sendToTaskExecutors,
                        pulsarOutputs.sendToMetricsPerName
                    )
                }
                println(" done")
            }
        }
    }

    private fun CoroutineScope.startWorkflowEngineWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.workflowEngine?.let {
            if (it.modeOrDefault == Mode.worker) {
                // storage decorated by a cache and a logger
                val storage = CachedKeyValueStorage(
                    it.stateCacheOrDefault.getKeyValueCache(config),
                    it.stateStorage!!.getKeyValueStorage(config)
                )
                print(
                    "Workflow engine".padEnd(25) + ": starting ${it.consumers} instances... " +
                        "(storage: ${it.stateStorage}, cache:${it.stateCacheOrDefault})"
                )
                repeat(it.consumersOrDefault) { counter ->
                    logger.info("InfiniticWorker - starting workflow engine {}", counter)
                    startPulsarWorkflowEngineWorker(
                        counter,
                        pulsarConsumerFactory.newWorkflowEngineConsumer(consumerName, counter),
                        storage,
                        pulsarOutputs.sendEventsToClient,
                        pulsarOutputs.sendEventsToTagEngine,
                        pulsarOutputs.sendCommandsToTaskEngine,
                        pulsarOutputs.sendEventsToWorkflowEngine
                    )
                }
                println(" done")
            }
        }
    }

    private fun CoroutineScope.startMonitoringWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.monitoring?.let {
            if (it.mode == Mode.worker) {
                // storage decorated by the cache
                val storage = CachedKeyValueStorage(
                    it.stateCacheOrDefault.getKeyValueCache(config),
                    it.stateStorage!!.getKeyValueStorage(config)
                )
                repeat(it.consumersOrDefault) { counter ->
                    logger.info("InfiniticWorker - starting monitoring per name {}", counter)
                    startPulsarMonitoringPerNameWorker(
                        counter,
                        pulsarConsumerFactory.newMonitoringPerNameEngineConsumer(consumerName, counter),
                        storage,
                        pulsarOutputs.sendToMetricsGlobal
                    )
                }

                logger.info("InfiniticWorker - starting monitoring global")
                startPulsarMonitoringGlobalWorker(
                    pulsarConsumerFactory.newMonitoringGlobalEngineConsumer(consumerName),
                    storage
                )
            }
        }
    }

    private fun CoroutineScope.startTaskExecutorWorkers(
        consumerName: String,
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
        val taskExecutorRegister = TaskExecutorRegisterImpl()

        for (workflow in config.workflows) {
            if (workflow.modeOrDefault == Mode.worker) {
                taskExecutorRegister.register(workflow.name) { workflow.instance }

                repeat(workflow.consumers) {
                    print("Workflow executor".padEnd(25) + ": starting ${workflow.concurrency} instances for ${workflow.name}...")
                    logger.info("InfiniticWorker - starting workflow executor for {}", workflow.name)
                    startPulsarTaskExecutorWorker(
                        dispatcher,
                        workflow.name,
                        it,
                        pulsarConsumerFactory.newWorkflowExecutorConsumer(consumerName, it, workflow.name),
                        pulsarOutputs.sendEventsToTaskEngine,
                        taskExecutorRegister,
                        workflow.concurrency
                    )
                    println(" done")
                }
            }
        }

        for (task in config.tasks) {
            if (task.modeOrDefault == Mode.worker) {
                taskExecutorRegister.register(task.name) { task.instance }

                repeat(task.consumers) {
                    print("Task executor".padEnd(25) + ": starting ${task.concurrency} instances for ${task.name}...")
                    logger.info("InfiniticWorker - starting task executor for {}", task.name)
                    startPulsarTaskExecutorWorker(
                        dispatcher,
                        task.name,
                        it,
                        pulsarConsumerFactory.newTaskExecutorConsumer(consumerName, it, task.name),
                        pulsarOutputs.sendEventsToTaskEngine,
                        taskExecutorRegister,
                        task.concurrency
                    )
                    println(" done")
                }
            }
        }
    }
}
