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
package io.infinitic.dashboard.panels.infrastructure

import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.requests.Request
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.state.KVar
import mu.KotlinLogging
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

private val logger = KotlinLogging.logger {}

private const val NAMES_DELAY = 30000L
private const val STATS_DELAY = 5000L

typealias JobStats = Map<String, Request<PartitionedTopicStats>>

typealias JobNames = Request<Set<String>>

abstract class AllJobsState(
    open val names: JobNames,
    open val stats: JobStats,
) {
  companion object {
    fun isLoading(names: JobNames, stats: JobStats): Boolean =
        stats.any { it.value.isLoading } || names.isLoading

    fun lastUpdatedAt(names: JobNames, stats: JobStats): Instant {
      val namesLastUpdated = names.lastUpdated
      val statsLastUpdated = stats.maxOfOrNull { it.value.lastUpdated }

      return when (statsLastUpdated) {
        null -> namesLastUpdated
        else ->
            when (statsLastUpdated.isAfter(namesLastUpdated)) {
              true -> statsLastUpdated
              false -> namesLastUpdated
            }
      }
    }
  }

  fun namesLoading() = create(names = names.copyLoading())
  fun statsLoading() = create(stats = stats.mapValues { it.value.copyLoading() })

  abstract fun create(names: JobNames = this.names, stats: JobStats = this.stats): AllJobsState

  abstract fun getNames(): Set<String>
  abstract fun getPartitionedStats(name: String): PartitionedTopicStats
}

@Suppress("UNCHECKED_CAST")
fun <T : AllJobsState> CoroutineScope.update(kvar: KVar<T>) = launch {
  while (isActive) {
    with(kvar) {
      val namesDelay = launch { delay(NAMES_DELAY) }

      // update task names every NAMES_UPDATE_DELAY millis
      try {
        logger.debug { "Updating names" }
        // loading indicator
        value = value.namesLoading() as T
        // request Pulsar
        val names = value.getNames()
        value =
            value.create(
                names = Completed(names),
                stats = names.associateWith { value.stats.getOrDefault(it, Loading()) }) as T
      } catch (e: Exception) {
        value =
            value.create(
                names = Failed(e),
                stats = mapOf(),
            ) as T
      }

      // update task stats every STATS_UPDATE_DELAY millis
      when (val names = value.names) {
        is Completed ->
            while (namesDelay.isActive) {
              val delayStats = launch { delay(STATS_DELAY) }
              // loading indicator
              value = value.statsLoading() as T
              // update stats
              val stats = mutableMapOf<String, Request<PartitionedTopicStats>>()
              names.result.map {
                logger.debug { "Updating executor stats for $it" }
                try {
                  stats[it] = Completed(value.getPartitionedStats(it))
                } catch (e: Exception) {
                  stats[it] = Failed(e)
                }
              }
              // update array of stats
              value = value.create(stats = stats) as T
              // wait at least delayStats
              delayStats.join()
            }
        else -> {
          namesDelay.cancel()
          delay(STATS_DELAY)
        }
      }
    }
  }
}
