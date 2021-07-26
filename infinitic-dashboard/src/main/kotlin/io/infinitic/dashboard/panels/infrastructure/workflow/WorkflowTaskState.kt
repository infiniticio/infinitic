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
import io.infinitic.dashboard.panels.infrastructure.jobs.JobState
import io.infinitic.dashboard.panels.infrastructure.jobs.TopicsStats
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import java.time.Instant

data class WorkflowTaskState(
    override val name: String,
    override val topicsStats: TopicsStats<WorkflowTaskTopic> = WorkflowTaskTopic.values().associateWith { Loading() },
    val isLoading: Boolean = isLoading(topicsStats),
    val lastUpdatedAt: Instant = lastUpdatedAt(topicsStats)
) : JobState<WorkflowTaskTopic>(name, topicsStats) {
    override fun create(name: String, topicsStats: TopicsStats<WorkflowTaskTopic>) =
        WorkflowTaskState(name = name, topicsStats = topicsStats)

    override fun getTopic(type: WorkflowTaskTopic) = topicName.of(type, name)
}
