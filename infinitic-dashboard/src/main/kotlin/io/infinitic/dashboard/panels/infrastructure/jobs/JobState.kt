/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.dashboard.panels.infrastructure.jobs

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.dashboard.Infinitic.topics
import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.Request
import io.infinitic.pulsar.resources.TopicType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.state.KVar
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.time.Instant

private const val UPDATE_DELAY = 5000L

private val logger = KotlinLogging.logger {}

typealias TopicsStats<T> = Map<T, Request<PartitionedTopicStats>>

abstract class JobState<T : TopicType>(
  open val name: String,
  open val topicsStats: TopicsStats<T>,
) {
  abstract fun create(name: String = this.name, topicsStats: TopicsStats<T>): JobState<T>

  companion object {
    fun <T> isLoading(topicsStats: TopicsStats<T>): Boolean = topicsStats.any { it.value.isLoading }

    fun <T> lastUpdatedAt(topicsStats: TopicsStats<T>): Instant =
        topicsStats.maxOfOrNull { it.value.lastUpdated } ?: Instant.now()
  }

  abstract fun getTopic(type: T): String

  fun statsLoading() = create(topicsStats = topicsStats.mapValues { it.value.copyLoading() })
}

internal fun <S : TopicType, T : JobState<S>> CoroutineScope.update(kvar: KVar<T>) = launch {
  while (isActive) {
    with(kvar) {
      val delay = launch { delay(UPDATE_DELAY) }
      logger.debug { "Updating stats for ${value.name}" }
      // loading indicator
      @Suppress("UNCHECKED_CAST")
      value = value.statsLoading() as T
      // request stats one by one
      val topicsStats = mutableMapOf<S, Request<PartitionedTopicStats>>()
      value.topicsStats.forEach {
        try {
          val stats = topics.getPartitionedStats(value.getTopic(it.key), true)
          topicsStats[it.key] = Completed(stats)
        } catch (e: Exception) {
          topicsStats[it.key] = Failed(e)
        }
      }
      // set value
      @Suppress("UNCHECKED_CAST")
      value =
          value.create(
              topicsStats = topicsStats,
          ) as T
      // wait at least UPDATE_DELAY
      delay.join()
    }
  }
}
