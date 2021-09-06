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

import io.infinitic.common.data.Name
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.metrics.global.engine.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import io.infinitic.pulsar.topics.GlobalTopic
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
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
import io.infinitic.tags.tasks.storage.TaskTagStorage
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.worker.InfiniticWorker
import io.infinitic.worker.config.WorkerConfig
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.api.PulsarClient

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PulsarInfiniticWorker private constructor(
    val pulsarClient: PulsarClient,
    val pulsarAdmin: PulsarAdmin,
    override val workerConfig: WorkerConfig
) : InfiniticWorker(workerConfig) {

    companion object {
        /**
         * Create PulsarInfiniticWorker from a custom PulsarClient and PulsarAdmin and a WorkerConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, pulsarAdmin: PulsarAdmin, workerConfig: WorkerConfig) =
            PulsarInfiniticWorker(pulsarClient, pulsarAdmin, workerConfig)

        /**
         * Create PulsarInfiniticWorker from a WorkerConfig instance
         */
        @JvmStatic
        fun fromConfig(workerConfig: WorkerConfig): PulsarInfiniticWorker =
            PulsarInfiniticWorker(workerConfig.pulsar!!.client, workerConfig.pulsar!!.admin, workerConfig)

        /**
         * Create PulsarInfiniticWorker from a config in resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(WorkerConfig.fromResource(*resources))

        /**
         * Create PulsarInfiniticWorker from a config in system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(WorkerConfig.fromFile(*files))
    }

    val pulsar = workerConfig.pulsar!!

    override val name by lazy {
        getProducerName(pulsarClient, pulsar.tenant, pulsar.namespace, workerConfig.name)
    }

    private val pulsarConsumerFactory by lazy {
        PulsarConsumerFactory(pulsarClient, pulsar.tenant, pulsar.namespace)
    }

    private val pulsarOutput by lazy {
        PulsarOutput.from(pulsarClient, pulsar.tenant, pulsar.namespace, name)
    }

    private val clientFactory = { PulsarInfiniticClient(pulsarClient, pulsarAdmin, pulsar.tenant, pulsar.namespace) }

    /**
     * Close worker
     */
    override fun close() {
        super.close()

        pulsarClient.close()
        pulsarAdmin.close()
    }

    override fun start() {
        // make sure all needed topics exists
        runningScope.future {
            val namer = TopicName(pulsar.tenant, pulsar.namespace)
            val topics = pulsarAdmin.topics().getPartitionedTopicList("${pulsar.tenant}/${pulsar.namespace}")

            GlobalTopic.values().forEach {
                val topicName = namer.of(it)
                if (!topics.contains(topicName)) {
                    logger.warn { "Creation of topic: $topicName" }
                    pulsarAdmin.topics().createPartitionedTopicAsync(topicName, 1)
                }
            }

            for (workflow in workerConfig.workflows) {

                WorkflowTopic.values().forEach {
                    val topicName = namer.of(it, workflow.name)
                    if (!topics.contains(topicName)) {
                        logger.warn { "Creation of topic: $topicName" }
                        pulsarAdmin.topics().createPartitionedTopicAsync(topicName, 1)
                    }
                }

                WorkflowTaskTopic.values().forEach {
                    val topicName = namer.of(it, workflow.name)
                    if (!topics.contains(topicName)) {
                        logger.warn { "Creation of topic: $topicName" }
                        pulsarAdmin.topics().createPartitionedTopicAsync(topicName, 1)
                    }
                }
            }

            for (task in workerConfig.tasks) {
                TaskTopic.values().forEach {
                    val topicName = namer.of(it, task.name)
                    if (!topics.contains(topicName)) {
                        logger.warn { "Creation of topic: $topicName" }
                        pulsarAdmin.topics().createPartitionedTopicAsync(topicName, 1)
                    }
                }
            }
        }.join()

        super.start()
    }
    override fun CoroutineScope.startTaskExecutors(name: Name, concurrency: Int) {
        startPulsarTaskExecutors(
            name,
            concurrency,
            this@PulsarInfiniticWorker.name,
            taskExecutorRegister,
            pulsarConsumerFactory,
            pulsarOutput,
            clientFactory
        )
    }

    override fun CoroutineScope.startWorkflowTagEngines(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: WorkflowTagStorage
    ) {
        startPulsarWorkflowTagEngines(
            workflowName,
            concurrency,
            storage,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startTaskEngines(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: TaskStateStorage
    ) {
        startPulsarTaskEngines(
            workflowName,
            concurrency,
            storage,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startTaskEngines(taskName: TaskName, concurrency: Int, storage: TaskStateStorage) {
        startPulsarTaskEngines(
            taskName,
            concurrency,
            storage,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startTaskDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        startPulsarTaskDelayEngines(
            workflowName,
            concurrency,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startTaskDelayEngines(taskName: TaskName, concurrency: Int) {
        startPulsarTaskDelayEngines(
            taskName,
            concurrency,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startWorkflowEngines(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: WorkflowStateStorage
    ) {
        startPulsarWorkflowEngines(
            workflowName,
            concurrency,
            storage,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startWorkflowDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        startPulsarWorkflowDelayEngines(
            workflowName,
            concurrency,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startTaskTagEngines(taskName: TaskName, concurrency: Int, storage: TaskTagStorage) {
        startPulsarTaskTagEngines(
            taskName,
            concurrency,
            storage,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startMetricsPerNameEngines(taskName: TaskName, storage: MetricsPerNameStateStorage) {
        startPulsarMetricsPerNameEngines(
            taskName,
            storage,
            name,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    override fun CoroutineScope.startMetricsGlobalEngine(storage: MetricsGlobalStateStorage) {
        startPulsarMetricsGlobalEngine(
            storage,
            name,
            pulsarConsumerFactory
        )
    }
}
