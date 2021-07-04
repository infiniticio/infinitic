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

package io.infinitic.dashboard.panels.infrastructure.workflow

import io.infinitic.dashboard.Infinitic.topicName
import io.infinitic.dashboard.Infinitic.topics
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.state.KVar
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.time.Instant

data class InfraWorkflowTaskState(
    val workflowName: String,
    val workflowTaskTopicsStats: Map<WorkflowTaskTopic, PartitionedTopicStats?> =
        WorkflowTaskTopic.values().map { it }.associateWith { null },
    val lastUpdated: Instant = Instant.now()
)

fun KVar<InfraWorkflowTaskState>.update(scope: CoroutineScope) = scope.launch {
    while (isActive) {
        val delay = launch { delay(3000) }
        println("UPDATING STATS FOR WORKFLOW_TASK ${value.workflowName}")

        var workflowTaskTopicsStats = value.workflowTaskTopicsStats
        value.workflowTaskTopicsStats.keys.forEach {
            try {
                val stats = topics.getPartitionedStats(topicName.of(it, value.workflowName), true, true, true)
                workflowTaskTopicsStats = workflowTaskTopicsStats.plus(it to stats)
            } catch (e: Exception) {
                println("error with $it")
            }
        }
        value = value.copy(
            workflowTaskTopicsStats = workflowTaskTopicsStats,
            lastUpdated = Instant.now()
        )

        delay.join()
    }
}
