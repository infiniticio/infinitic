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

package io.infinitic.dashboard.panels.pulsar

import io.infinitic.dashboard.Infinitic
import io.infinitic.dashboard.Infinitic.topicName
import io.infinitic.dashboard.Infinitic.topics
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kweb.state.KVar
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

data class PulsarWorkflowsState(
    val workflowNames: Set<String>? = null,
    val workflowTaskExecutorsStats: Map<String, PartitionedTopicStats?> = mapOf()
)

fun KVar<PulsarWorkflowsState>.init(scope: CoroutineScope) = scope.launch {
    delay((0..10000).random().toLong())
    // get set of workflow names
    val workflowNames = Infinitic.admin.workflows
    // update state with workflow names
    value = value.copy(
        workflowNames = workflowNames,
        workflowTaskExecutorsStats = workflowNames.associateWith { null }
    )
    // update state of each workflow
    workflowNames.map { updateWorkflowTaskExecutorStats(scope, it) }
}

private fun KVar<PulsarWorkflowsState>.updateWorkflowTaskExecutorStats(scope: CoroutineScope, workflowName: String) = scope.launch {
    delay((0..10000).random().toLong())
    val topic = topicName.of(WorkflowTaskTopic.EXECUTORS, workflowName)
    try {
        val stats = topics.getPartitionedStats(topic, true, true, true)
        value = value.copy(workflowTaskExecutorsStats = value.workflowTaskExecutorsStats.plus(workflowName to stats))
        println("$value")
    } catch (e: Exception) {
        println("error with $topic")
    }
}
