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
import io.infinitic.pulsar.config.AdminConfig
import io.infinitic.transport.pulsar.config.Pulsar
import io.infinitic.transport.pulsar.topics.PerNameTopics
import io.infinitic.transport.pulsar.topics.TaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTopics
import mu.KotlinLogging
import org.apache.pulsar.client.admin.Clusters
import org.apache.pulsar.client.admin.Namespaces
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.admin.Tenants
import org.apache.pulsar.client.admin.Topics
import org.apache.pulsar.common.policies.data.Policies
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.apache.pulsar.common.policies.data.SchemaCompatibilityStrategy
import org.apache.pulsar.common.policies.data.TenantInfo
import org.apache.pulsar.common.policies.data.TopicType
import org.apache.pulsar.common.policies.data.impl.AutoTopicCreationOverrideImpl
import org.apache.pulsar.common.policies.data.impl.DelayedDeliveryPoliciesImpl
import java.io.Closeable

@Suppress("unused", "MemberVisibilityCanBePrivate")
class PulsarInfiniticAdmin constructor(
    val pulsarAdmin: PulsarAdmin,
    val pulsar: Pulsar
) : Closeable {

    private val pulsarClusters: Clusters = pulsarAdmin.clusters()
    private val pulsarTopics: Topics = pulsarAdmin.topics()
    private val pulsarTenants: Tenants = pulsarAdmin.tenants()
    private val pulsarNamespaces: Namespaces = pulsarAdmin.namespaces()

    val topicName = PerNameTopics(pulsar.tenant, pulsar.namespace)

    private val fullNamespace = "${pulsar.tenant}/${pulsar.namespace}"

    private val policies: Policies by lazy {
        Policies().apply {
            // all new topics (especially tasks and workflows) are partitioned
            autoTopicCreationOverride = AutoTopicCreationOverrideImpl(
                false,
                TopicType.PARTITIONED.toString(),
                1
            )
            // schema are mandatory for producers/consumers
            schema_validation_enforced = true
            // this allow topic auto creation for task / workflows
            is_allow_auto_update_schema = true
            // Changes allowed: add optional fields, delete fields
            schema_compatibility_strategy = SchemaCompatibilityStrategy.BACKWARD_TRANSITIVE
            // default topic policies
            deduplicationEnabled = pulsar.policies.deduplicationEnabled
            // retention policies
            retention_policies = RetentionPolicies(
                pulsar.policies.retentionTimeInMinutes,
                pulsar.policies.retentionSizeInMB
            )
            message_ttl_in_seconds = pulsar.policies.messageTTLInSeconds
            delayed_delivery_policies = DelayedDeliveryPoliciesImpl(
                pulsar.policies.delayedDeliveryTickTimeMillis,
                true
            )
        }
    }

    private val tenantInfo by lazy {
        TenantInfo.builder()
            .allowedClusters(
                when (pulsar.allowedClusters) {
                    null -> pulsarClusters.clusters.toSet()
                    else -> pulsar.allowedClusters
                }
            )
            .also {
                if (pulsar.adminRoles != null) it.adminRoles(pulsar.adminRoles)
            }
            .build()
    }

    companion object {
        internal val logger = KotlinLogging.logger { }

        /**
         * Create InfiniticAdmin from a custom PulsarAdmin and an AdminConfig instance
         */
        @JvmStatic
        fun from(pulsarAdmin: PulsarAdmin, adminConfig: AdminConfig) = PulsarInfiniticAdmin(
            pulsarAdmin,
            adminConfig.pulsar
        )

        /**
         * Create InfiniticAdmin from an AdminConfig instance
         */
        @JvmStatic
        fun fromConfig(adminConfig: AdminConfig): PulsarInfiniticAdmin =
            from(adminConfig.pulsar.admin, adminConfig)

        /**
         * Create InfiniticAdmin from file in resources directory
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(AdminConfig.fromResource(*resources))

        /**
         * Create InfiniticAdmin from file in system file
         */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(AdminConfig.fromFile(*files))
    }

    /**
     * Set of topics for current tenant and namespace
     */
    val topics: Set<String> by lazy {
        pulsarTopics.getPartitionedTopicList(fullNamespace).toSet()
    }

    /**
     * Set of task's names for current tenant and namespace
     */
    val tasks: Set<String> by lazy {
        val tasks = mutableSetOf<String>()
        val prefix = topicName.topic(TaskTopics.EXECUTOR, TaskName(""))
        topics.map { if (it.startsWith(prefix)) tasks.add(it.removePrefix(prefix)) }

        tasks
    }

    /**
     * Set of workflow's names for current tenant and namespace
     */
    val workflows: Set<String> by lazy {
        val workflows = mutableSetOf<String>()
        val prefix = topicName.topic(WorkflowTopics.ENGINE, WorkflowName(""))
        topics.map { if (it.startsWith(prefix)) workflows.add(it.removePrefix(prefix)) }

        workflows
    }

    /**
     * Create Pulsar tenant
     *
     * Returns true if the tenant already existed
     */
    fun createTenant(): Boolean = try {
        logger.info { "Creating tenant '${pulsar.tenant}'" }
        pulsarTenants.createTenant(pulsar.tenant, tenantInfo)
        false
    } catch (e: PulsarAdminException.ConflictException) {
        logger.debug { "Already existing tenant '${pulsar.tenant}'" }
        true
    } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized - make sure by yourself that tenant '${pulsar.tenant}' exists - with the right info" }
        false
    } catch (e: PulsarAdminException.NotAllowedException) {
        logger.warn { "Not allowed - make sure by yourself that tenant '${pulsar.tenant}' exists - with the right info" }
        false
    }

    /**
     * Create Pulsar namespace
     *
     * Returns true if the namespace already existed
     */
    fun createNamespace(): Boolean = try {
        logger.debug { "Creating namespace $fullNamespace" }
        pulsarNamespaces.createNamespace(fullNamespace, policies)
        false
    } catch (e: PulsarAdminException.ConflictException) {
        logger.debug { "Already existing namespace '$fullNamespace'" }
        true
    } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized - make sure by yourself that namespace '$fullNamespace' exists - with the right policies" }
        false
    } catch (e: PulsarAdminException.NotAllowedException) {
        logger.warn { "Not allowed - make sure by yourself that namespace '$fullNamespace' exists - with the right policies" }
        false
    }

    /**
     * Create Pulsar topic
     *
     * Returns true if the topic already existed
     */
    fun createTopic(topic: String, isPartitioned: Boolean): Boolean = try {
        logger.debug { "Creating topic $topic" }
        when (isPartitioned) {
            true -> pulsarTopics.createPartitionedTopic(topic, 3)
            false -> pulsarTopics.createNonPartitionedTopic(topic)
        }
        false
    } catch (e: PulsarAdminException.ConflictException) {
        logger.debug { "Already existing topic '$topic'" }
        true
    } catch (e: PulsarAdminException.NotAllowedException) {
        logger.warn { "Not allowed - make sure by yourself that topic '$topic' exists - with the right policies" }
        false
    } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized - make sure by yourself that topic '$topic' exists - with the right policies" }
        false
    }

    /**
     * Update policies for namespace
     */
    fun updateNamespacePolicies() {
        with(pulsarNamespaces) {
            try {
                logger.info { "Updating policy AutoTopicCreation to ${policies.autoTopicCreationOverride}" }
                setAutoTopicCreation(fullNamespace, policies.autoTopicCreationOverride)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update autoTopicCreationOverride policy: ${e.message}" }
            }
            try {
                logger.info { "Updating policy SchemaValidationEnforced to ${policies.schema_validation_enforced}" }
                setSchemaValidationEnforced(fullNamespace, policies.schema_validation_enforced)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update schema_validation_enforced policy: ${e.message}" }
            }
            try {
                logger.info { "Updating policy IsAllowAutoUpdateSchema to ${policies.is_allow_auto_update_schema}" }
                setIsAllowAutoUpdateSchema(fullNamespace, policies.is_allow_auto_update_schema)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update is_allow_auto_update_schema policy: ${e.message}" }
            }
            try {
                logger.info { "Updating policy SchemaCompatibilityStrategy to ${policies.schema_compatibility_strategy}" }
                setSchemaCompatibilityStrategy(fullNamespace, policies.schema_compatibility_strategy)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update schema_compatibility_strategy policy: ${e.message}" }
            }
            try {
                logger.info { "Updating policy DeduplicationStatus to ${policies.deduplicationEnabled}" }
                setDeduplicationStatus(fullNamespace, policies.deduplicationEnabled)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update deduplicationEnabled policy: ${e.message}" }
            }
            try {
                logger.info { "Updating policy Retention to ${policies.retention_policies}" }
                setRetention(fullNamespace, policies.retention_policies)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update namespace's retention_policies: ${e.message}" }
            }
            try {
                logger.info { "Updating policy NamespaceMessageTTL to ${policies.message_ttl_in_seconds}" }
                setNamespaceMessageTTL(fullNamespace, policies.message_ttl_in_seconds)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update namespace's message_ttl_in_seconds policy: ${e.message}" }
            }
            try {
                logger.info { "Updating policy DelayedDeliveryMessages to ${policies.delayed_delivery_policies}" }
                setDelayedDeliveryMessages(fullNamespace, policies.delayed_delivery_policies)
            } catch (e: PulsarAdminException) {
                logger.warn { "Failing to update namespace's delayed_delivery_policies policy: ${e.message}" }
            }
        }
    }

    /**
     * Close Pulsar client
     */
    override fun close() = pulsarAdmin.close()

    /**
     * Prints stats for workflows and tasks topics
     */
    fun printTopicStats() {
        // get list of all topics
        val line = "+--------------------------------------------+-------------+------------+------------+%n"
        val title = "| Subscription                               | NbConsumers | MsgBacklog | MsgRateOut |%n"

        println("WORKFLOWS")
        println()

        workflows.forEach { workflow ->
            println(workflow)

            System.out.format(line)
            System.out.format(title)
            System.out.format(line)

            WorkflowTopics.values().forEach {
                displayStatsTopic(topicName.topic(it, WorkflowName(workflow)))
            }

            WorkflowTaskTopics.values().forEach {
                displayStatsTopic(topicName.topic(it, WorkflowName(workflow)))
            }

            System.out.format(line)
            println()
        }

        // print tasks stats
        println("TASKS")
        println()

        tasks.forEach { task ->
            println(task)

            System.out.format(line)
            System.out.format(title)
            System.out.format(line)

            TaskTopics.values().forEach {
                displayStatsTopic(topicName.topic(it, TaskName(task)))
            }

            System.out.format(line)
            println()
        }
    }

    private fun displayStatsTopic(topic: String) {
        val stats = pulsarTopics.getPartitionedStats(topic, true, true, true)

        val format = "| %-42s | %11d | %10d | %10f |%n"

        stats.subscriptions.map {
            System.out.format(
                format,
                it.key,
                it.value.consumers.size,
                it.value.msgBacklog,
                it.value.msgRateOut
            )
        }
    }
}
