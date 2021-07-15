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
import io.infinitic.dashboard.panels.infrastructure.InfraTopicStats
import io.infinitic.dashboard.panels.infrastructure.jobs.InfraJobState
import io.infinitic.pulsar.topics.WorkflowTopic
import java.time.Instant

data class InfraWorkflowState(
    override val name: String,
    override val topicsStats: Map<WorkflowTopic, InfraTopicStats> =
        WorkflowTopic.values().map { it }.associateWith { InfraTopicStats(topicName.of(it, name)) },
    override val lastUpdated: Instant = Instant.now()
) : InfraJobState<WorkflowTopic> {
    override fun create(name: String, topicsStats: Map<WorkflowTopic, InfraTopicStats>, lastUpdated: Instant) =
        InfraWorkflowState(name, topicsStats, lastUpdated)
}
