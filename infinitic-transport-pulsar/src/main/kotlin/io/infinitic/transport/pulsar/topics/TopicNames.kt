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

interface TopicNames {
    val tenant: String
    val namespace: String

    fun fullName(topic: String) = "persistent://$tenant/$namespace/$topic"

    fun producerName(workerName: String, type: TopicType): String

    fun consumerName(workerName: String, type: TopicType): String

    fun topic(type: GlobalTopics): String

    fun topic(type: ClientTopics, clientName: ClientName): String

    fun topic(type: WorkflowTopics, workflowName: WorkflowName): String
    fun topicDLQ(type: WorkflowTopics, workflowName: WorkflowName): String

    fun topic(type: TaskTopics, taskName: TaskName): String
    fun topicDLQ(type: TaskTopics, taskName: TaskName): String

    fun topic(type: WorkflowTaskTopics, workflowName: WorkflowName): String
    fun topicDLQ(type: WorkflowTaskTopics, workflowName: WorkflowName): String

    fun topic(type: TopicType, name: String) = when (type) {
        is ClientTopics -> topic(type, ClientName(name))
        is GlobalTopics -> topic(type)
        is WorkflowTopics -> topic(type, WorkflowName(name))
        is TaskTopics -> topic(type, TaskName(name))
        is WorkflowTaskTopics -> topic(type, WorkflowName(name))
    }

    fun topicDLQ(type: TopicType, name: String) = when (type) {
        is WorkflowTopics -> topicDLQ(type, WorkflowName(name))
        is TaskTopics -> topicDLQ(type, TaskName(name))
        is WorkflowTaskTopics -> topicDLQ(type, WorkflowName(name))
        else -> null
    }
}
