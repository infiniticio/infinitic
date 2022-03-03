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

package io.infinitic.transport.pulsar.topics

import io.infinitic.common.data.ClientName
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.transport.pulsar.topics.Topics.Companion.CLIENT
import io.infinitic.transport.pulsar.topics.Topics.Companion.GLOBAL_METRICS
import io.infinitic.transport.pulsar.topics.Topics.Companion.GLOBAL_NAMER
import io.infinitic.transport.pulsar.topics.Topics.Companion.TASK_DELAY
import io.infinitic.transport.pulsar.topics.Topics.Companion.TASK_ENGINE
import io.infinitic.transport.pulsar.topics.Topics.Companion.TASK_EXECUTOR
import io.infinitic.transport.pulsar.topics.Topics.Companion.TASK_METRICS
import io.infinitic.transport.pulsar.topics.Topics.Companion.TASK_TAG
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_DELAY
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_ENGINE
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_METRICS
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_TAG
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_TASK_DELAY
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_TASK_ENGINE
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_TASK_EXECUTOR
import io.infinitic.transport.pulsar.topics.Topics.Companion.WORKFLOW_TASK_METRICS

class PerNameTopics(override val tenant: String, override val namespace: String) : Topics {

    override fun global() = Topics.GlobalTopics(
        metrics = fullName(GLOBAL_METRICS),
        namer = fullName(GLOBAL_NAMER)
    )

    override fun clients(clientName: ClientName) = Topics.ClientTopics(
        response = fullName("$CLIENT: $clientName"),
    )

    override fun workflows(workflowName: WorkflowName) = Topics.WorkflowTopics(
        tag = fullName("$WORKFLOW_TAG: $workflowName"),
        engine = fullName("$WORKFLOW_ENGINE: $workflowName"),
        delay = fullName("$WORKFLOW_DELAY: $workflowName"),
        metrics = fullName("$WORKFLOW_METRICS: $workflowName")
    )

    override fun tasks(taskName: TaskName) = Topics.TaskTopics(
        tag = fullName("$TASK_TAG: $taskName"),
        engine = fullName("$TASK_ENGINE: $taskName"),
        delay = fullName("$TASK_DELAY: $taskName"),
        metrics = fullName("$TASK_METRICS: $taskName"),
        executor = fullName("$TASK_EXECUTOR: $taskName")
    )

    override fun workflowTasks(workflowName: WorkflowName) = Topics.WorkflowTaskTopics(
        engine = fullName("$WORKFLOW_TASK_ENGINE: $workflowName"),
        delay = fullName("$WORKFLOW_TASK_DELAY: $workflowName"),
        metrics = fullName("$WORKFLOW_TASK_METRICS: $workflowName"),
        executor = fullName("$WORKFLOW_TASK_EXECUTOR: $workflowName")
    )
}
