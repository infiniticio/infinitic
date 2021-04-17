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
import io.infinitic.config.AdminConfig
import io.infinitic.config.loaders.loadConfigFromFile
import io.infinitic.config.loaders.loadConfigFromResource
import io.infinitic.pulsar.admin.setupInfinitic
import io.infinitic.pulsar.topics.TopicNamer
import io.infinitic.pulsar.topics.TopicType
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticAdmin(
    @JvmField val pulsarAdmin: PulsarAdmin,
    @JvmField val tenant: String,
    @JvmField val namespace: String,
    @JvmField val allowedClusters: Set<String>? = null
) {
    private val topicNamer = TopicNamer(tenant, namespace)

    companion object {
        /*
        Create InfiniticAdmin from an AdminConfig
         */
        @JvmStatic
        fun fromConfig(config: AdminConfig): InfiniticAdmin {
            // build PulsarAdmin from config
            val pulsarAdmin = PulsarAdmin
                .builder()
                .serviceHttpUrl(config.pulsar.serviceHttpUrl)
                .allowTlsInsecureConnection(true)
                .build()

            return InfiniticAdmin(
                pulsarAdmin,
                config.pulsar.tenant,
                config.pulsar.namespace,
                config.pulsar.allowedClusters
            )
        }

        /*
        Create InfiniticAdmin from an AdminConfig loaded from a resource
         */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
       Create InfiniticAdmin from an AdminConfig loaded from a file
        */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
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
            val prefix = topicNamer.taskEngineTopic(TopicType.COMMANDS, TaskName(""))
            topics.map { if (it.startsWith(prefix)) tasks.add(it.removePrefix(prefix)) }

            return tasks
        }

    /**
     * Set of workflow's names for current tenant and namespace
     */
    val workflows: Set<String>
        get() {
            val workflows = mutableSetOf<String>()
            val prefix = topicNamer.workflowEngineTopic(TopicType.COMMANDS, WorkflowName(""))
            topics.map { if (it.startsWith(prefix)) workflows.add(it.removePrefix(prefix)) }

            return workflows
        }

    /**
     * Create Pulsar tenant and namespace if it does not exist, with adhoc settings
     */
    fun setupPulsar() = runBlocking { pulsarAdmin.setupInfinitic(tenant, namespace, allowedClusters) }

    /**
     * Close Pulsar client
     */
    fun close() = pulsarAdmin.close()

    /**
     * Prints stats for workflows and tasks topics
     */
    fun printTopicStats() {
        // get list of all topics
        val leftAlignFormat = "| %-20s | %-8s | %11d | %10d | %10f | %7d |%n"
        val line = "+----------------------+----------+-------------+------------+------------+---------+%n"
        val title = "| Subscription         | Type     | NbConsumers | MsgBacklog | MsgRateOut | Unacked |%n"

        workflows.forEach {
            println("Workflow: $it")

            System.out.format(line)
            System.out.format(title)
            System.out.format(line)

            // workflow engine commands
            var topic = topicNamer.workflowEngineTopic(TopicType.COMMANDS, WorkflowName(it))
            var stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("commands", stats, leftAlignFormat)

            // workflow engine events
            topic = topicNamer.workflowEngineTopic(TopicType.EVENTS, WorkflowName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("events", stats, leftAlignFormat)

            // tag workflow engine commands
            topic = topicNamer.tagEngineTopic(TopicType.COMMANDS, WorkflowName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("commands", stats, leftAlignFormat)

            // tag workflow engine events
            topic = topicNamer.tagEngineTopic(TopicType.EVENTS, WorkflowName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("events", stats, leftAlignFormat)

            // workflow tasks engine commands
            topic = topicNamer.taskEngineTopic(TopicType.COMMANDS, WorkflowName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("commands", stats, leftAlignFormat)

            // workflow tasks engine events
            topic = topicNamer.taskEngineTopic(TopicType.EVENTS, WorkflowName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("events", stats, leftAlignFormat)

            // workflow executors
            topic = topicNamer.executorTopic(WorkflowName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("", stats, leftAlignFormat)

            System.out.format(line)
            println("")
        }

        // print tasks stats
        tasks.forEach {
            println("Task: $it")

            System.out.format(line)
            System.out.format(title)
            System.out.format(line)

            // task engine commands
            var topic = topicNamer.taskEngineTopic(TopicType.COMMANDS, TaskName(it))
            var stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("commands", stats, leftAlignFormat)

            // task engine events
            topic = topicNamer.taskEngineTopic(TopicType.EVENTS, TaskName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("events", stats, leftAlignFormat)

            // tag task engine commands
            topic = topicNamer.tagEngineTopic(TopicType.COMMANDS, TaskName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("commands", stats, leftAlignFormat)

            // tag task engine events
            topic = topicNamer.tagEngineTopic(TopicType.EVENTS, TaskName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("events", stats, leftAlignFormat)

            // task executors
            topic = topicNamer.executorTopic(TaskName(it))
            stats = pulsarAdmin.topics().getPartitionedStats(topic, true, true, true)
            displayStatsLine("", stats, leftAlignFormat)

            System.out.format(line)
            println("")
        }
    }

    private fun displayStatsLine(title: String, stats: PartitionedTopicStats, format: String) {
        stats.subscriptions.map {
            System.out.format(
                format,
                it.key,
                title,
                it.value.consumers.size,
                it.value.msgBacklog,
                it.value.msgRateOut,
                it.value.unackedMessages
            )
        }
    }
}
