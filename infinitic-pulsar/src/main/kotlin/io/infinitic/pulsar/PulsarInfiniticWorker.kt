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

import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.transport.pulsar.PulsarWorkerStarter
import io.infinitic.transport.pulsar.config.Pulsar
import io.infinitic.transport.pulsar.topics.GlobalTopics
import io.infinitic.transport.pulsar.topics.PerNameTopics
import io.infinitic.transport.pulsar.topics.TaskTopics
import io.infinitic.transport.pulsar.topics.TopicNames
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTopics
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.WorkerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import org.apache.pulsar.client.admin.Namespaces
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.admin.Tenants
import org.apache.pulsar.client.admin.Topics
import org.apache.pulsar.client.api.PulsarClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.system.exitProcess

@Suppress("MemberVisibilityCanBePrivate", "unused")
class PulsarInfiniticWorker private constructor(
    val pulsarClient: PulsarClient,
    val pulsarAdmin: PulsarAdmin,
    override val workerConfig: WorkerConfig
) : InfiniticWorker(workerConfig) {

    companion object {
        /**
         * Create [PulsarInfiniticWorker] from custom PulsarClient and PulsarAdmin and a WorkerConfig instance
         */
        @JvmStatic
        fun from(pulsarClient: PulsarClient, pulsarAdmin: PulsarAdmin, workerConfig: WorkerConfig) =
            PulsarInfiniticWorker(pulsarClient, pulsarAdmin, workerConfig)

        /**
         * Create [PulsarInfiniticWorker] from a WorkerConfig instance
         */
        @JvmStatic
        fun fromConfig(workerConfig: WorkerConfig): PulsarInfiniticWorker =
            PulsarInfiniticWorker(workerConfig.pulsar!!.client, workerConfig.pulsar!!.admin, workerConfig)

        /**
         * Create [PulsarInfiniticWorker] from a config in resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(WorkerConfig.fromResource(*resources))

        /**
         * Create [PulsarInfiniticWorker] from a config in system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(WorkerConfig.fromFile(*files))
    }

    /**
     * Thread pool used to run workers
     */
    private val runningThreadPool = Executors.newCachedThreadPool()

    /**
     * [CoroutineScope] used to run workers
     */
    private val runningScope = CoroutineScope(runningThreadPool.asCoroutineDispatcher() + Job())

    /**
     * [Pulsar] configuration
     */
    val pulsar: Pulsar = workerConfig.pulsar!!

    /**
     * [PulsarInfiniticAdmin] instance: used to create tenant, namespace, topics, etc.
     */
    val infiniticAdmin by lazy { PulsarInfiniticAdmin(pulsarAdmin, pulsar) }

    /**
     * [TopicNames] instance: used to get topic's name
     */
    private val topics: TopicNames = PerNameTopics(pulsar.tenant, pulsar.namespace)

    /**
     * Worker unique name: from workerConfig or generated through Pulsar
     */
    override val name by lazy {
        getProducerName(pulsarClient, topics, workerConfig.name)
    }

    private val fullNamespace = "${pulsar.tenant}/${pulsar.namespace}"

    override val workerStarter by lazy {
        PulsarWorkerStarter(topics, pulsarClient, name)
    }

    override val clientFactory = {
        PulsarInfiniticClient(pulsarClient, pulsarAdmin, pulsar.tenant, pulsar.namespace)
    }

    /**
     * Start worker synchronously
     */
    override fun start(): Unit = startAsync().join()

    /**
     * Start worker asynchronously
     */
    override fun startAsync(): CompletableFuture<Unit> = runningScope.future {
        try {
            // check that tenant exists or create it
            pulsarAdmin.tenants().checkOrCreateTenant()
            // check that namespace exists or create it
            pulsarAdmin.namespaces().checkOrCreateNamespace()
            // check that topics exist or create them
            pulsarAdmin.topics().checkOrCreateTopics()
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

        start()
    }

    /**
     * Close worker
     */
    override fun close() {
        runningScope.cancel()
        runningThreadPool.shutdown()

        pulsarClient.close()
        pulsarAdmin.close()
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

    private fun Topics.checkOrCreateTopics() {
        val topics = PerNameTopics(pulsar.tenant, pulsar.namespace)

        // get current existing topics
        val existing: MutableList<String> = try {
            logger.debug { "Getting list of partitioned topics for namespace $fullNamespace" }
            getPartitionedTopicList(fullNamespace)
        } catch (e: PulsarAdminException.NotAllowedException) {
            logger.warn { "Not allowed to get list of topics for $fullNamespace: ${e.message}" }
            mutableListOf()
        } catch (e: PulsarAdminException.NotAuthorizedException) {
            logger.warn { "Not authorized to get list of topics for $fullNamespace: ${e.message}" }
            mutableListOf()
        }

        val checkOrCreateTopic = { topic: String ->
            if (! existing.contains(topic)) {
                try {
                    logger.debug { "Creating topic $topic" }
                    createPartitionedTopic(topic, 1)
                    existing.add(topic)
                } catch (e: PulsarAdminException.ConflictException) {
                    logger.warn { "Topic already exists: $topic: ${e.message}" }
                    existing.add(topic)
                } catch (e: PulsarAdminException.NotAllowedException) {
                    logger.warn { "Not allowed to create topic $topic: ${e.message}" }
                } catch (e: PulsarAdminException.NotAuthorizedException) {
                    logger.warn { "Not authorized to create topic $topic: ${e.message}" }
                }
            }
        }

        GlobalTopics.values().forEach {
            checkOrCreateTopic(topics.topic(it))
        }

        for (task in workerConfig.tasks) {
            val taskName = TaskName(task.name)
            TaskTopics.values().forEach {
                checkOrCreateTopic(topics.topic(it, taskName))
            }
        }

        for (workflow in workerConfig.workflows) {
            val workflowName = WorkflowName(workflow.name)
            WorkflowTopics.values().forEach {
                checkOrCreateTopic(topics.topic(it, workflowName))
            }
            WorkflowTaskTopics.values().forEach {
                checkOrCreateTopic(topics.topic(it, workflowName))
            }
        }
    }
}
