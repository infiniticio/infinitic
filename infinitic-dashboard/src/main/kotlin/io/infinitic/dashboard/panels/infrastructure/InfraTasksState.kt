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
import io.infinitic.pulsar.topics.TaskTopic
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

data class InfraTasksState(
    val taskNames: JobNames = JobNames(),
    val taskStats: Map<String, TopicStats> = mapOf(),
    val lastUpdated: Instant = Instant.now()
)

fun KVar<InfraTasksState>.update(scope: CoroutineScope) = scope.launch {
    while (isActive) {
        val delayJob = launch { delay(NAMES_UPDATE_DELAY) }

        // update task names every NAMES_UPDATE_DELAY millis
        try {
            logger.debug { "Updating task names" }
            // request Pulsar
            val taskNames = Infinitic.admin.tasks
            val taskStats = value.taskStats
            value = value.copy(
                taskNames = JobNames(
                    request = Completed(taskNames),
                    lastUpdated = Instant.now()
                ),
                taskStats = taskNames.associateWith {
                    when (taskStats.containsKey(it)) {
                        true -> taskStats[it]!!
                        false -> TopicStats(topic = getExecutorTopicForTask(it))
                    }
                }
            )
        } catch (e: Exception) {
            value = value.copy(
                taskNames = JobNames(
                    request = Failed(e),
                    lastUpdated = Instant.now()
                ),
                taskStats = mapOf(),
                lastUpdated = Instant.now()
            )
        }

        // update task stats every STATS_UPDATE_DELAY millis
        val updateJob = launch {
            while (isActive) {
                val delay = launch { delay(STATS_UPDATE_DELAY) }
                var taskStats = value.taskStats
                val request = value.taskNames.request
                if (request is Completed) {
                    request.result.map {
                        logger.debug { "Updating executor stats for $it" }
                        val topic = getExecutorTopicForTask(it)
                        try {
                            val stats = Infinitic.topics.getPartitionedStats(topic, true, true, true)
                            taskStats = taskStats.plus(
                                it to TopicStats(
                                    topic = topic,
                                    request = Completed(stats)
                                )
                            )
                        } catch (e: Exception) {
                            taskStats = taskStats.plus(
                                it to TopicStats(
                                    topic = topic,
                                    request = Failed(e)
                                )
                            )
                        }
                    }
                }
                // update array of stats
                value = value.copy(
                    taskStats = taskStats,
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

private fun getExecutorTopicForTask(taskName: String) = Infinitic.topicName.of(TaskTopic.EXECUTORS, taskName)
