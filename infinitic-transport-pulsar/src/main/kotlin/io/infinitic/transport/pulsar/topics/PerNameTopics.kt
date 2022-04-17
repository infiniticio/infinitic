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

class PerNameTopics(override val tenant: String, override val namespace: String) : TopicNames {

    override fun producerName(workerName: String, type: TopicType) =
        "$workerName>>${type.subscriptionPrefix}"

    override fun consumerName(workerName: String, type: TopicType) =
        "$workerName<<${type.subscriptionPrefix}"

    override fun topic(type: ClientTopics, clientName: ClientName) =
        fullName("${type.subscriptionPrefix}:$clientName")

    override fun topic(type: GlobalTopics) = fullName(type.subscriptionPrefix)

    override fun topic(type: WorkflowTopics, workflowName: WorkflowName) =
        fullName("${type.subscriptionPrefix}:$workflowName")
    override fun topicDLQ(type: WorkflowTopics, workflowName: WorkflowName) =
        fullName("${type.subscriptionPrefix}-dlq:$workflowName")

    override fun topic(type: WorkflowTaskTopics, workflowName: WorkflowName) =
        fullName("${type.subscriptionPrefix}:$workflowName")
    override fun topicDLQ(type: WorkflowTaskTopics, workflowName: WorkflowName) =
        fullName("${type.subscriptionPrefix}-dlq:$workflowName")

    override fun topic(type: TaskTopics, taskName: TaskName) =
        fullName("${type.subscriptionPrefix}:$taskName")
    override fun topicDLQ(type: TaskTopics, taskName: TaskName) =
        fullName("${type.subscriptionPrefix}-dlq:$taskName")
}
