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

package io.infinitic.dashboard.panels.infrastructure.jobs

import io.infinitic.dashboard.Infinitic.topics
import io.infinitic.dashboard.panels.infrastructure.InfraStatus
import io.infinitic.dashboard.panels.infrastructure.InfraTopicStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.state.KVar
import mu.KotlinLogging
import java.time.Instant

private const val UPDATE_DELAY = 5000L

private val logger = KotlinLogging.logger {}

internal fun <S, T : InfraJobState<S>> CoroutineScope.update(kvar: KVar<T>) = launch {
    while (isActive) {
        with(kvar) {
            val delay = launch { delay(UPDATE_DELAY) }
            logger.debug { "Updating stats for ${value.name}" }

            val map = mutableMapOf<S, InfraTopicStats>()
            value.topicsStats.forEach {
                try {
                    val stats = topics.getPartitionedStats(it.value.topic, true, true, true)
                    map[it.key] = InfraTopicStats(
                        status = InfraStatus.COMPLETED,
                        topic = it.value.topic,
                        partitionedTopicStats = stats,
                    )
                } catch (e: Exception) {
                    map[it.key] = InfraTopicStats(
                        status = InfraStatus.ERROR,
                        topic = it.value.topic,
                        stackTrace = e.stackTraceToString()
                    )
                    logger.error { "Error while requesting PartitionedTopicStats for workflow task ${value.name}" }
                    logger.error { e.printStackTrace() }
                }
            }

            value = value.create(
                topicsStats = map,
                lastUpdated = Instant.now(),
            ) as T

            delay.join()
        }
    }
}

interface InfraJobState<T> {
    val name: String
    val topicsStats: Map<T, InfraTopicStats>
    val lastUpdated: Instant

    fun create(
        name: String = this.name,
        topicsStats: Map<T, InfraTopicStats> = this.topicsStats,
        lastUpdated: Instant = this.lastUpdated
    ): InfraJobState<T>
}
