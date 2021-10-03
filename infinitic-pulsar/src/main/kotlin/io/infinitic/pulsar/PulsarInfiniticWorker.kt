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
import kotlinx.coroutines.launch
import org.apache.pulsar.client.admin.Namespaces
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.admin.Tenants
import org.apache.pulsar.client.admin.Topics
import org.apache.pulsar.client.api.PulsarClient
import java.util.concurrent.CompletableFuture
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

    val infiniticAdmin by lazy { PulsarInfiniticAdmin(pulsarAdmin, pulsar) }

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

    /**
     * Start worker
     */
    override fun startAsync(): CompletableFuture<Unit> {
        // make sure all needed topics exists
        runningScope.future {
            try {
                // check that tenant exists or create it
                pulsarAdmin.tenants().checkOrCreateTenant()
                // check that namespace exists or create it
                pulsarAdmin.namespaces().checkOrCreateNamespace()
                // check that topics exist or create them
                checkOrCreateTopics()
            } catch (e: Exception) {
                logger.error(e) {
                    when (e) {
                        is PulsarAdminException.NotAuthorizedException -> "Not authorized - check your credentials"
                        else -> Unit
                    }
                }
                close()
                exitProcess(1)
            }
        }.join()

        return super.startAsync()
    }

    private fun Tenants.checkOrCreateTenant() {
        try {
            infiniticAdmin.createTenant()
        } catch (e: PulsarAdminException.NotAllowedException) {
            logger.warn { "Not allowed to get info for tenant ${pulsar.tenant}: ${e.message}" }
        } catch (e: PulsarAdminException.NotAuthorizedException) {
            logger.warn { "Not authorized to get info for tenant ${pulsar.tenant}: ${e.message}" }
        }
    }

    private fun Namespaces.checkOrCreateNamespace() {
        val existing = try {
            ! infiniticAdmin.createNamespace()
        } catch (e: PulsarAdminException.NotAllowedException) {
            logger.warn { "Not allowed to get policies for namespace $fullNamespace: ${e.message}" }
            true
        } catch (e: PulsarAdminException.NotAuthorizedException) {
            logger.warn { "Not authorized to get policies for namespace $fullNamespace: ${e.message}" }
            true
        }

        if (existing) {
            // already existing namespace
            try {
                if (pulsar.policies.forceUpdate) infiniticAdmin.updatePolicies()
            } catch (e: PulsarAdminException.NotAllowedException) {
                logger.warn { "Not allowed to set policies for namespace $fullNamespace: ${e.message}" }
            } catch (e: PulsarAdminException.NotAuthorizedException) {
                logger.warn { "Not authorized to set policies for namespace $fullNamespace: ${e.message}" }
            }
        }
    }

    private fun CoroutineScope.checkOrCreateTopics() {
        val topicName = TopicName(pulsar.tenant, pulsar.namespace)

        // get current existing topics
        val topics = pulsarAdmin.topics().getPartitionedTopicList()

        GlobalTopic.values().forEach {
            val name = topicName.of(it)
            if (!topics.contains(name)) {
                logger.info { "Creation of topic: $name" }
                launch {
                    pulsarAdmin.topics().createInfiniticPartitionedTopic(name)
                }
            }
        }

        for (workflow in workerConfig.workflows) {
            WorkflowTopic.values().forEach {
                val name = topicName.of(it, workflow.name)
                if (!topics.contains(name)) {
                    logger.info { "Creation of topic: $name" }
                    launch {
                        pulsarAdmin.topics().createInfiniticPartitionedTopic(name)
                    }
                }
            }
            WorkflowTaskTopic.values().forEach {
                val name = topicName.of(it, workflow.name)
                if (!topics.contains(name)) {
                    logger.info { "Creation of topic: $name" }
                    launch {
                        pulsarAdmin.topics().createInfiniticPartitionedTopic(name)
                    }
                }
            }
        }

        for (task in workerConfig.tasks) {
            TaskTopic.values().forEach {
                val name = topicName.of(it, task.name)
                if (!topics.contains(name)) {
                    logger.info { "Creation of topic: $name" }
                    launch {
                        pulsarAdmin.topics().createInfiniticPartitionedTopic(name)
                    }
                }
            }
        }
    }

    private fun Topics.getPartitionedTopicList(): MutableList<String> {
        return try {
            getPartitionedTopicList(fullNamespace)
        } catch (e: PulsarAdminException.NotAllowedException) {
            logger.warn { "Not allowed to get list of topics for $fullNamespace: ${e.message}" }
            mutableListOf()
        } catch (e: PulsarAdminException.NotAuthorizedException) {
            logger.warn { "Not authorized to get list of topics for $fullNamespace: ${e.message}" }
            mutableListOf()
        }
    }

    private fun Topics.createInfiniticPartitionedTopic(topicName: String) {
        try {
            createPartitionedTopic(topicName, 1)
        } catch (e: PulsarAdminException.ConflictException) {
            logger.warn { "Topic already exists: $topicName" }
        }
    }

    override fun startTaskExecutors(name: Name, concurrency: Int) {
        runningScope.launch {
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
    }

    override fun startWorkflowTagEngines(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: WorkflowTagStorage
    ) {
        runningScope.launch {
            startPulsarWorkflowTagEngines(
                workflowName,
                concurrency,
                storage,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startTaskEngines(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: TaskStateStorage
    ) {
        runningScope.launch {
            startPulsarTaskEngines(
                workflowName,
                concurrency,
                storage,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startTaskEngines(taskName: TaskName, concurrency: Int, storage: TaskStateStorage) {
        runningScope.launch {
            startPulsarTaskEngines(
                taskName,
                concurrency,
                storage,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startTaskDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        runningScope.launch {
            startPulsarTaskDelayEngines(
                workflowName,
                concurrency,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startTaskDelayEngines(taskName: TaskName, concurrency: Int) {
        runningScope.launch {
            startPulsarTaskDelayEngines(
                taskName,
                concurrency,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startWorkflowEngines(
        workflowName: WorkflowName,
        concurrency: Int,
        storage: WorkflowStateStorage
    ) {
        runningScope.launch {
            startPulsarWorkflowEngines(
                workflowName,
                concurrency,
                storage,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startWorkflowDelayEngines(workflowName: WorkflowName, concurrency: Int) {
        runningScope.launch {
            startPulsarWorkflowDelayEngines(
                workflowName,
                concurrency,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startTaskTagEngines(taskName: TaskName, concurrency: Int, storage: TaskTagStorage) {
        runningScope.launch {
            startPulsarTaskTagEngines(
                taskName,
                concurrency,
                storage,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startMetricsPerNameEngines(taskName: TaskName, storage: MetricsPerNameStateStorage) {
        runningScope.launch {
            startPulsarMetricsPerNameEngines(
                taskName,
                storage,
                name,
                pulsarConsumerFactory,
                pulsarOutput
            )
        }
    }

    override fun startMetricsGlobalEngine(storage: MetricsGlobalStateStorage) {
        runningScope.launch {
            startPulsarMetricsGlobalEngine(
                storage,
                name,
                pulsarConsumerFactory
            )
        }
    }
}
