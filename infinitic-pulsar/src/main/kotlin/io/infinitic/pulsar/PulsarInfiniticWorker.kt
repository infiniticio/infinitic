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
import io.infinitic.pulsar.topics.TOPIC_WITH_DELAYS
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import io.infinitic.pulsar.topics.createInfiniticPartitionedTopic
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
import kotlinx.coroutines.launch
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.api.PulsarClient
import kotlin.system.exitProcess

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

    private val fullNamespace = "${pulsar.tenant}/${pulsar.namespace}"

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
            // check that tenant exists or create it
            checkTenant()
            // check that namespace exists or create it
            checkNamespace()

            val topicName = TopicName(pulsar.tenant, pulsar.namespace)

            // check that namespace has valid policies or try to apply them at topic level
            val topicPolicy = when (
                with(pulsarAdmin.namespaces().getPolicies(fullNamespace)) {
                    retention_policies == null ||
                        (retention_policies.retentionTimeInMinutes == 0 && retention_policies.retentionSizeInMB == 0L) ||
                        deduplicationEnabled == null || deduplicationEnabled == false
                }
            ) {
                true -> pulsar.topicPolicy
                false -> null
            }

            // get current existing topics
            val topics = pulsarAdmin.topics().getPartitionedTopicList(fullNamespace)

            GlobalTopic.values().forEach {
                val name = topicName.of(it)
                if (!topics.contains(name)) {
                    logger.warn { "Creation of topic: $name" }
                    launch {
                        pulsarAdmin.topics().createInfiniticPartitionedTopic(
                            name,
                            it.prefix.contains(TOPIC_WITH_DELAYS),
                            topicPolicy
                        )
                    }
                }
            }

            for (workflow in workerConfig.workflows) {
                WorkflowTopic.values().forEach {
                    val name = topicName.of(it, workflow.name)
                    if (!topics.contains(name)) {
                        logger.warn { "Creation of topic: $name" }
                        launch {
                            pulsarAdmin.topics().createInfiniticPartitionedTopic(
                                name,
                                it.prefix.contains(TOPIC_WITH_DELAYS),
                                topicPolicy,
                            )
                        }
                    }
                }
                WorkflowTaskTopic.values().forEach {
                    val name = topicName.of(it, workflow.name)
                    if (!topics.contains(name)) {
                        logger.warn { "Creation of topic: $name" }
                        launch {
                            pulsarAdmin.topics().createInfiniticPartitionedTopic(
                                name,
                                it.prefix.contains(TOPIC_WITH_DELAYS),
                                topicPolicy
                            )
                        }
                    }
                }
            }

            for (task in workerConfig.tasks) {
                TaskTopic.values().forEach {
                    val name = topicName.of(it, task.name)
                    if (!topics.contains(name)) {
                        logger.warn { "Creation of topic: $name" }
                        launch {
                            pulsarAdmin.topics().createInfiniticPartitionedTopic(
                                name,
                                it.prefix.contains(TOPIC_WITH_DELAYS),
                                topicPolicy
                            )
                        }
                    }
                }
            }
        }.join()

        super.start()
    }

    private fun checkTenant() {
        try {
            pulsarAdmin.tenants().getTenantInfo(pulsar.tenant)
        } catch (e: PulsarAdminException.NotFoundException) {
            logger.warn { "Tenant ${pulsar.tenant} does not exist." }
            try {
                PulsarInfiniticAdmin(pulsarAdmin, pulsar).createTenant()
            } catch (e: Exception) {
                logger.error(e) {}
                close()
                exitProcess(1)
            }
        }
    }

    private fun checkNamespace() {
        try {
            pulsarAdmin.namespaces().getPolicies(fullNamespace)
        } catch (e: PulsarAdminException.NotFoundException) {
            logger.warn { "Namespace $fullNamespace does not exist." }
            try {
                PulsarInfiniticAdmin(pulsarAdmin, pulsar).createNamespace()
            } catch (e: Exception) {
                logger.error(e) {}
                close()
                exitProcess(1)
            }
        }
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
