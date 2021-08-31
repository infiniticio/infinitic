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

import io.infinitic.pulsar.admin.setupInfinitic
import io.infinitic.pulsar.config.AdminConfig
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.io.Closeable

@Suppress("MemberVisibilityCanBePrivate", "unused")

class PulsarInfiniticAdmin @JvmOverloads constructor(
    val pulsarAdmin: PulsarAdmin,
    val tenant: String,
    val namespace: String,
    val allowedClusters: Set<String>? = null,
    val adminRoles: Set<String>? = null
) : Closeable {
    val topicNamer = TopicName(tenant, namespace)

    companion object {
        /**
         * Create InfiniticAdmin from a custom PulsarAdmin and an AdminConfig instance
         */
        @JvmStatic
        fun from(pulsarAdmin: PulsarAdmin, adminConfig: AdminConfig) = PulsarInfiniticAdmin(
            pulsarAdmin,
            adminConfig.pulsar.tenant,
            adminConfig.pulsar.namespace,
            adminConfig.pulsar.allowedClusters,
            adminConfig.pulsar.adminRoles
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
    val topics: Set<String>
        get() = pulsarAdmin.topics().getPartitionedTopicList("$tenant/$namespace").toSet()

    /**
     * Set of task's names for current tenant and namespace
     */
    val tasks: Set<String>
        get() {
            val tasks = mutableSetOf<String>()
            val prefix = topicNamer.of(TaskTopic.EXECUTORS, "")
            topics.map { if (it.startsWith(prefix)) tasks.add(it.removePrefix(prefix)) }

            return tasks
        }

    /**
     * Set of workflow's names for current tenant and namespace
     */
    val workflows: Set<String>
        get() {
            val workflows = mutableSetOf<String>()
            val prefix = topicNamer.of(WorkflowTaskTopic.EXECUTORS, "")
            topics.map { if (it.startsWith(prefix)) workflows.add(it.removePrefix(prefix)) }

            return workflows
        }

    /**
     * Create Pulsar tenant and namespace if it does not exist, with adhoc settings
     */
    fun setupPulsar() = runBlocking { pulsarAdmin.setupInfinitic(tenant, namespace, allowedClusters, adminRoles) }

    /**
     * Close Pulsar client
     */
    override fun close() = pulsarAdmin.close()

    /**
     * Prints stats for workflows and tasks topics
     */
    fun printTopicStats() {
        // get list of all topics
        val leftAlignFormat = "| %-42s | %11d | %10d | %10f |%n"
        val line = "+--------------------------------------------+-------------+------------+------------+%n"
        val title = "| Subscription                               | NbConsumers | MsgBacklog | MsgRateOut |%n"

        println("WORKFLOWS")
        println()

        workflows.forEach {
            println(it)

            System.out.format(line)
            System.out.format(title)
            System.out.format(line)

            // workflow-new engine
            var topic = topicNamer.of(WorkflowTopic.ENGINE_NEW, it)
            var stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // workflow-existing engine
            topic = topicNamer.of(WorkflowTopic.ENGINE_EXISTING, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // workflow-delays engine
            topic = topicNamer.of(WorkflowTopic.DELAYS, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // tag workflow-new engine
            topic = topicNamer.of(WorkflowTopic.TAG_NEW, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // tag workflow engine events
            topic = topicNamer.of(WorkflowTopic.TAG_EXISTING, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // workflow tasks-new engine
            topic = topicNamer.of(WorkflowTaskTopic.ENGINE_NEW, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // workflow tasks-existing engine
            topic = topicNamer.of(WorkflowTaskTopic.ENGINE_EXISTING, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // workflow executors
            topic = topicNamer.of(WorkflowTaskTopic.EXECUTORS, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            System.out.format(line)
            println("")
        }

        // print tasks stats
        println("TASKS")
        println()

        tasks.forEach {
            println(it)

            System.out.format(line)
            System.out.format(title)
            System.out.format(line)

            // task-new engine
            var topic = topicNamer.of(TaskTopic.ENGINE_NEW, it)
            var stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // task-existing engine
            topic = topicNamer.of(TaskTopic.ENGINE_EXISTING, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // task delays engine
            topic = topicNamer.of(TaskTopic.DELAYS, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // tag task-new engine
            topic = topicNamer.of(TaskTopic.TAG_NEW, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // tag task-existing engine
            topic = topicNamer.of(TaskTopic.TAG_EXISTING, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // task executors
            topic = topicNamer.of(TaskTopic.EXECUTORS, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            // task metrics
            topic = topicNamer.of(TaskTopic.METRICS, it)
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine(stats, leftAlignFormat)

            System.out.format(line)
            println("")
        }
    }

    private fun displayStatsLine(stats: PartitionedTopicStats, format: String) {
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
