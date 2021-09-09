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

import io.infinitic.pulsar.config.AdminConfig
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import io.infinitic.transport.pulsar.Pulsar
import mu.KotlinLogging
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.common.policies.data.AutoTopicCreationOverride
import org.apache.pulsar.common.policies.data.DelayedDeliveryPolicies
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import org.apache.pulsar.common.policies.data.Policies
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.apache.pulsar.common.policies.data.SchemaCompatibilityStrategy
import org.apache.pulsar.common.policies.data.TenantInfo
import org.apache.pulsar.common.policies.data.TopicType
import java.io.Closeable

@Suppress("MemberVisibilityCanBePrivate", "unused")

class PulsarInfiniticAdmin constructor(
    val pulsarAdmin: PulsarAdmin,
    val pulsar: Pulsar
) : Closeable {
    val topicName = TopicName(pulsar.tenant, pulsar.namespace)

    private val fullNamespace = "${pulsar.tenant}/${pulsar.namespace}"

    private val policies: Policies by lazy {
        Policies().apply {
            // all new topics (especially tasks and workflows) are partitioned
            autoTopicCreationOverride = AutoTopicCreationOverride(
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
            retention_policies = RetentionPolicies(
                pulsar.policies.retentionTimeInMinutes,
                pulsar.policies.retentionSizeInMB
            )
            message_ttl_in_seconds = pulsar.policies.messageTTLInSeconds
            delayed_delivery_policies = DelayedDeliveryPolicies(
                pulsar.policies.delayedDeliveryTickTimeMillis,
                true
            )
        }
    }

    private val tenantInfo by lazy {
        TenantInfo().apply {
            // if authorizedClusters is not provided, default is all clusters
            allowedClusters = when (pulsar.allowedClusters) {
                null -> pulsarAdmin.clusters().clusters.toSet()
                else -> pulsar.allowedClusters
            }
            // apply adminRoles if provided
            if (adminRoles != null) this.adminRoles = adminRoles
        }
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
        pulsarAdmin.topics().getPartitionedTopicList(fullNamespace).toSet()
    }

    /**
     * Set of task's names for current tenant and namespace
     */
    val tasks: Set<String> by lazy {
        val tasks = mutableSetOf<String>()
        val prefix = topicName.of(TaskTopic.EXECUTORS, "")
        topics.map { if (it.startsWith(prefix)) tasks.add(it.removePrefix(prefix)) }

        tasks
    }

    /**
     * Set of workflow's names for current tenant and namespace
     */
    val workflows: Set<String> by lazy {
        val workflows = mutableSetOf<String>()
        val prefix = topicName.of(WorkflowTaskTopic.EXECUTORS, "")
        topics.map { if (it.startsWith(prefix)) workflows.add(it.removePrefix(prefix)) }

        workflows
    }

    /**
     * Create Pulsar tenant
     */
    fun createTenant() {
        with(pulsarAdmin.tenants()) {
            try {
                logger.info { "checking if tenant ${pulsar.tenant} already exists by requesting its info" }
                getTenantInfo(pulsar.tenant)
                logger.warn { "Tenant ${pulsar.tenant} already exists" }
            } catch (e: PulsarAdminException.NotFoundException) {
                pulsarAdmin.tenants().createTenant(pulsar.tenant, tenantInfo)
            }
        }
    }

    /**
     * Create Pulsar namespace
     * Returns a boolean indicating if the namespace was actually created
     */
    fun createNamespace(): Boolean = with(pulsarAdmin.namespaces()) {
        try {
            logger.info { "checking if namespace $fullNamespace already exists by requesting its policies" }
            getPolicies(fullNamespace)
            logger.warn { "Namespace $fullNamespace already exists" }
            false
        } catch (e: PulsarAdminException.NotFoundException) {
            pulsarAdmin.namespaces().createNamespace(fullNamespace, policies)
            true
        }
    }

    /**
     * Update policies for namespace
     */
    fun updatePolicies() {
        with(pulsarAdmin.namespaces()) {
            setAutoTopicCreation(fullNamespace, policies.autoTopicCreationOverride)
            setSchemaValidationEnforced(fullNamespace, policies.schema_validation_enforced)
            setIsAllowAutoUpdateSchema(fullNamespace, policies.is_allow_auto_update_schema)
            setSchemaCompatibilityStrategy(fullNamespace, policies.schema_compatibility_strategy)
            setDeduplicationStatus(fullNamespace, policies.deduplicationEnabled)
            try {
                setRetention(fullNamespace, policies.retention_policies)
            } catch (e: PulsarAdminException.PreconditionFailedException) {
                // TODO check why
                logger.warn { "Failing to update namespace's retention policies" }
            }
            setNamespaceMessageTTL(fullNamespace, policies.message_ttl_in_seconds)
            setDelayedDeliveryMessages(fullNamespace, policies.delayed_delivery_policies)
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

            WorkflowTopic.values().forEach {
                val topic = topicName.of(it, workflow)
                val stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
                displayStatsLine(stats)
            }

            WorkflowTaskTopic.values().forEach {
                val topic = topicName.of(it, workflow)
                val stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
                displayStatsLine(stats)
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

            TaskTopic.values().forEach {
                val topic = topicName.of(it, task)
                val stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
                displayStatsLine(stats)
            }

            System.out.format(line)
            println()
        }
    }

    private fun displayStatsLine(stats: PartitionedTopicStats) {
        val format = "| %-42s | %11d | %10d | %10f |%n"

        stats.subscriptions.map {
            System.out.format(
                format,
                it.key,
                it.value.consumers.size,
                it.value.msgBacklog,
                it.value.msgRateOut,
            )
        }
    }
}
