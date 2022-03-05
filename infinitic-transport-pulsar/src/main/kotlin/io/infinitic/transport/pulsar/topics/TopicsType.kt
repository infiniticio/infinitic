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

interface TopicType {
    val prefix: String

    fun consumerName(clientName: ClientName) = "$clientName:$prefix"
}

enum class ClientTopics(override val prefix: String) : TopicType {
    RESPONSE("response")
}

enum class GlobalTopics(override val prefix: String) : TopicType {
    NAMER("global-namer")
}

enum class WorkflowTopics(override val prefix: String) : TopicType {
    TAG("workflow-tag"),
    ENGINE("workflow-engine"),
    DELAY("workflow-$TOPIC_WITH_DELAY"),
    METRICS("workflow-metrics")
}

enum class WorkflowTaskTopics(override val prefix: String) : TopicType {
    TAG("workflow-task-tag"),
    ENGINE("workflow-task-engine"),
    DELAY("workflow-task-$TOPIC_WITH_DELAY"),
    EXECUTOR("workflow-task-executors"),
    METRICS("workflow-task-metrics")
}

enum class TaskTopics(override val prefix: String) : TopicType {
    TAG("task-tag"),
    ENGINE("task-engine"),
    DELAY("task-$TOPIC_WITH_DELAY"),
    EXECUTOR("task-executors"),
    METRICS("task-metrics")
}
