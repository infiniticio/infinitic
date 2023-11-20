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

import io.infinitic.dashboard.Infinitic
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.pulsar.resources.ServiceType
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.time.Instant

data class AllTasksState(
  override val names: JobNames = Loading(),
  override val stats: JobStats = mapOf(),
  val isLoading: Boolean = isLoading(names, stats),
  val lastUpdatedAt: Instant = lastUpdatedAt(names, stats)
) : AllJobsState(names, stats) {

  override fun create(names: JobNames, stats: JobStats) =
      AllTasksState(names = names, stats = stats)

  override fun getNames() = Infinitic.resourceManager.serviceSet

  override fun getPartitionedStats(name: String): PartitionedTopicStats {
    val topic = Infinitic.resourceManager.getTopicName(name, ServiceType.EXECUTOR)

    return Infinitic.topics.getPartitionedStats(topic, true)
  }
}
