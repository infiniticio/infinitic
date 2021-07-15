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

package io.infinitic.pulsar.topics

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.Name
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import java.lang.RuntimeException

class TopicNamer(private val tenantName: String, private val namespace: String) {

    private val client = "client"
    private val task = "task"
    private val workflow = "workflow"

    private val executor = "executor"
    private val metrics = "metrics"

    private val events = "events"
    private val commands = "commands"
    private val deadLetters = "dead-letters"

    private val engine = "engine"
    private val tag = "tag"
    private val delay = "delay"

    fun clientTopic(clientName: ClientName) = "$client: $clientName"

    fun tagEngineTopic(type: TopicType, name: Name): String {
        val prefix = if (isWorkflow(name)) workflow else task

        return getPersistentTopicFullName(
            when (type) {
                TopicType.COMMANDS -> "$prefix-$tag-$engine-$commands: $name"
                TopicType.EVENTS -> "$prefix-$tag-$engine-$events: $name"
                TopicType.DEAD_LETTERS -> "$prefix-$tag-$engine-$deadLetters: $name"
            }
        )
    }

    fun workflowEngineTopic(type: TopicType, workflowName: WorkflowName) = getPersistentTopicFullName(
        when (type) {
            TopicType.COMMANDS -> "$workflow-$engine-$commands: $workflowName"
            TopicType.EVENTS -> "$workflow-$engine-$events: $workflowName"
            TopicType.DEAD_LETTERS -> "$workflow-$engine-$deadLetters: $workflowName"
        }
    )

    fun taskEngineTopic(type: TopicType, name: Name): String {
        val prefix = if (isWorkflow(name)) "$workflow-$task" else task

        return getPersistentTopicFullName(
            when (type) {
                TopicType.COMMANDS -> "$prefix-$engine-$commands: $name"
                TopicType.EVENTS -> "$prefix-$engine-$events: $name"
                TopicType.DEAD_LETTERS -> "$prefix-$engine-$deadLetters: $name"
            }
        )
    }

    fun delayEngineTopic(name: Name): String {
        val prefix = if (isWorkflow(name)) workflow else task

        return getPersistentTopicFullName("$prefix-$delay-$engine: $name")
    }

    fun executorTopic(name: Name): String {
        val prefix = if (isWorkflow(name)) workflow else task

        return getPersistentTopicFullName("$prefix-$executor: $name")
    }

    fun metricsTopic(name: Name): String {
        val prefix = if (isWorkflow(name)) workflow else task

        return getPersistentTopicFullName("$prefix-$metrics: $name")
    }

    fun globalMetricsTopic() = getPersistentTopicFullName("global-$metrics")

    private fun isWorkflow(name: Name) = when (name) {
        is WorkflowName -> true
        is TaskName -> false
        else -> throw RuntimeException("Unexpected Name type ${name::class} for $name")
    }

    private fun getPersistentTopicFullName(topic: String) =
        "persistent://$tenantName/$namespace/$topic"
}
