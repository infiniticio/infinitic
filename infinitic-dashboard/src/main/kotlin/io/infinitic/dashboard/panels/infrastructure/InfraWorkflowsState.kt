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
import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.JobNames
import io.infinitic.dashboard.panels.infrastructure.requests.TopicStats
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.state.KVar
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

private const val NAMES_UPDATE_DELAY = 30000L
private const val STATS_UPDATE_DELAY = 5000L

data class InfraWorkflowsState(
    val workflowNames: JobNames = JobNames(),
    val workflowStats: Map<String, TopicStats?> = mapOf(),
    val lastUpdated: Instant = Instant.now()
)

fun KVar<InfraWorkflowsState>.update(scope: CoroutineScope) = scope.launch {
    while (isActive) {
        val delayJob = launch { delay(NAMES_UPDATE_DELAY) }

        // update workflow names every NAMES_UPDATE_DELAY millis
        try {
            logger.debug { "Updating workflow names" }
            // request Pulsar
            val workflowNames = Infinitic.admin.workflows
            val workflowStats = value.workflowStats
            value = value.copy(
                workflowNames = JobNames(
                    request = Completed(workflowNames),
                    lastUpdated = Instant.now()
                ),
                workflowStats = workflowNames.associateWith {
                    when (workflowStats.containsKey(it)) {
                        true -> workflowStats[it]!!
                        false -> TopicStats(topic = getExecutorTopicForWorkflow(it))
                    }
                }
            )
        } catch (e: Exception) {
            value = value.copy(
                workflowNames = JobNames(
                    request = Failed(e),
                    lastUpdated = Instant.now()
                ),
                workflowStats = mapOf(),
                lastUpdated = Instant.now()
            )
//            logger.error { "Error while updating workflow names" }
//            logger.error { e.printStackTrace() }
        }

        // update workflow stats every STATS_UPDATE_DELAY millis
        val updateJob = launch {
            while (isActive) {
                val delay = launch { delay(STATS_UPDATE_DELAY) }
                var workflowStats = value.workflowStats
                val request = value.workflowNames.request
                if (request is Completed) {
                    request.result.map {
                        logger.debug { "Updating executor stats for $it" }
                        val topic = getExecutorTopicForWorkflow(it)
                        try {
                            val stats = Infinitic.topics.getPartitionedStats(topic, true, true, true)
                            workflowStats = workflowStats.plus(
                                it to TopicStats(
                                    topic = topic,
                                    request = Completed(stats)
                                )
                            )
                        } catch (e: Exception) {
                            workflowStats = workflowStats.plus(
                                it to TopicStats(
                                    topic = topic,
                                    request = Failed(e)
                                )
                            )
//                            logger.error { "Error while updating executor stats for workflow $it" }
//                            logger.error { e.printStackTrace() }
                        }
                    }
                }
                // update array of stats
                value = value.copy(
                    workflowStats = workflowStats,
                    lastUpdated = Instant.now()
                )
                // wait at least delay
                delay.join()
            }
        }
        // wait for at least 30s
        delayJob.join()
        // cancel updateJob before updating taskNames
        updateJob.cancelAndJoin()
    }
}

private fun getExecutorTopicForWorkflow(taskName: String) = Infinitic.topicName.of(WorkflowTaskTopic.EXECUTORS, taskName)
