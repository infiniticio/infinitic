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

const val TOPIC_WITH_DELAY = "delays"

interface Topics {

    val tenant: String
    val namespace: String

    fun fullName(topic: String) = "persistent://$tenant/$namespace/$topic"

    data class GlobalTopics(
        val metrics: String,
        val namer: String
    )

    data class ClientTopics(
        val response: String
    )

    data class WorkflowTopics(
        val tag: String,
        val engine: String,
        val delay: String,
        val metrics: String
    )

    data class TaskTopics(
        val tag: String,
        val engine: String,
        val delay: String,
        val metrics: String,
        val executor: String
    )

    data class WorkflowTaskTopics(
        val engine: String,
        val delay: String,
        val metrics: String,
        val executor: String
    )

    fun global(): GlobalTopics
    fun clients(clientName: ClientName): ClientTopics
    fun workflows(workflowName: WorkflowName): WorkflowTopics
    fun tasks(taskName: TaskName): TaskTopics
    fun workflowTasks(workflowName: WorkflowName): WorkflowTaskTopics

    companion object {
        const val GLOBAL_METRICS = "global-metrics"
        const val GLOBAL_NAMER = "global-namer"

        const val CLIENT = "client"

        const val WORKFLOW_TAG = "workflow-tag"
        const val WORKFLOW_ENGINE = "workflow-engine"
        const val WORKFLOW_DELAY = "workflow-$TOPIC_WITH_DELAY"
        const val WORKFLOW_METRICS = "workflow-metrics"

        const val TASK_TAG = "task-tag"
        const val TASK_ENGINE = "task-engine"
        const val TASK_DELAY = "task-$TOPIC_WITH_DELAY"
        const val TASK_METRICS = "task-metrics"
        const val TASK_EXECUTOR = "task-executors"

        const val WORKFLOW_TASK_ENGINE = "workflow-task-engine"
        const val WORKFLOW_TASK_DELAY = "workflow-task-$TOPIC_WITH_DELAY"
        const val WORKFLOW_TASK_METRICS = "workflow-task-metrics"
        const val WORKFLOW_TASK_EXECUTOR = "workflow-task-executors"
    }
}
