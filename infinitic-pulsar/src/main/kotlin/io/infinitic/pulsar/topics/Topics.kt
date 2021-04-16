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

package io.infinitic.pulsar.topics

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.Name
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import java.lang.RuntimeException

private const val client = "client"
private const val task = "task"
private const val workflow = "workflow"

private const val executor = "executor"
private const val metrics = "metrics"

private const val events = "events"
private const val commands = "commands"
private const val deadLetters = "dead-letters"

private const val engine = "engine"
private const val tag = "tag"

fun clientTopic(clientName: ClientName) = "$client: $clientName"

fun tagEngineTopic(type: TopicType, workflowName: WorkflowName) = when (type) {
    TopicType.COMMANDS -> "$workflow-$tag-$engine-$commands: $workflowName"
    TopicType.EVENTS -> "$workflow-$tag-$engine-$events: $workflowName"
    TopicType.DEAD_LETTERS -> "$workflow-$tag-$engine-$deadLetters: $workflowName"
}

fun workflowEngineTopic(type: TopicType, workflowName: WorkflowName) = when (type) {
    TopicType.COMMANDS -> "$workflow-$engine-$commands: $workflowName"
    TopicType.EVENTS -> "$workflow-$engine-$events: $workflowName"
    TopicType.DEAD_LETTERS -> "$workflow-$engine-$deadLetters: $workflowName"
}

fun taskTagEngineTopic(type: TopicType, taskName: TaskName) = when (type) {
    TopicType.COMMANDS -> "$task-$tag-$engine-$commands: $taskName"
    TopicType.EVENTS -> "$task-$tag-$engine-$events: $taskName"
    TopicType.DEAD_LETTERS -> "$task-$tag-$engine-$deadLetters: $taskName"
}

fun taskEngineTopic(type: TopicType, name: Name): String {
    val prefix = when (name) {
        is TaskName -> task
        is WorkflowName -> "$workflow-$task"
        else -> throw RuntimeException("Unexpected name type: $name")
    }
    return when (type) {
        TopicType.COMMANDS -> "$prefix-$engine-$commands: $name"
        TopicType.EVENTS -> "$prefix-$engine-$events: $name"
        TopicType.DEAD_LETTERS -> "$prefix-$engine-$deadLetters: $name"
    }
}

fun taskExecutorTopic(name: Name): String {
    val prefix = when (name) {
        is TaskName -> task
        is WorkflowName -> "$workflow-$task"
        else -> throw RuntimeException("Unexpected name type: $name")
    }
    return "$prefix-$executor: $name"
}

fun taskMetricsTopic(taskName: TaskName) = "$task-$metrics: $taskName"

fun globalMetricsTopic() = "global-$metrics"
