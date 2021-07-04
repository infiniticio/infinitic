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

package io.infinitic.dashboard.panels.infrastructure

import io.infinitic.dashboard.Infinitic
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.state.KVar
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.time.Instant

data class InfraWorkflowsState(
    val workflowNames: Set<String>? = null,
    val workflowTaskExecutorsStats: Map<String, PartitionedTopicStats?> = mapOf(),
    val lastUpdated: Instant = Instant.now()
)

fun KVar<InfraWorkflowsState>.update(scope: CoroutineScope) = scope.launch {
    while (isActive) {
        val delayJob = launch { delay(30000) }

        // get set of workflow names
        try {
            println("UPDATING WORKFLOWS NAMES")
            // request Pulsar
            val workflowNames = Infinitic.admin.workflows
            val workflowTaskExecutorsStats = value.workflowTaskExecutorsStats
            value = value.copy(
                workflowNames = workflowNames,
                workflowTaskExecutorsStats = workflowNames.associateWith {
                    if (workflowTaskExecutorsStats.containsKey(it)) workflowTaskExecutorsStats[it] else null
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // update workflow stats every 3 seconds
        val updateJob = launch {
            while (isActive) {
                val delay = launch { delay(3000) }
                var workflowTaskExecutorsStats = value.workflowTaskExecutorsStats
                value.workflowNames?.map {
                    println("updating stats for $it")
                    val topic = Infinitic.topicName.of(WorkflowTaskTopic.EXECUTORS, it)
                    try {
                        val stats = Infinitic.topics.getPartitionedStats(topic, true, true, true)
                        workflowTaskExecutorsStats = workflowTaskExecutorsStats.plus(it to stats)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay.join()
                value = value.copy(
                    workflowTaskExecutorsStats = workflowTaskExecutorsStats,
                    lastUpdated = Instant.now()
                )
            }
        }
        // wait for at least 30s
        delayJob.join()
        // cancel updateJob before updating workflowNames
        updateJob.cancelAndJoin()
    }
}
