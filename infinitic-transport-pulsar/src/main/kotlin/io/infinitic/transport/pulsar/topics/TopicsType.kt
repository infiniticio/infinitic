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

import org.apache.pulsar.client.api.SubscriptionType

/**
 * must NOT be changed
 * (would change the name of the subscriptions of delayed messages)
 */
const val TOPIC_WITH_DELAY = "delays"

interface TopicType {
    /**
     * The subscriptionPrefix must NOT be changed
     * (if subscription name is changed, all messages will appear as not acknowledged to a new worker!)
     */
    val subscriptionPrefix: String

    /**
     * The subscriptionName must NOT be changed
     * (if subscription name is changed, all messages will appear as not acknowledged to a new worker!)
     */
    val subscriptionName: String get() = "${subscriptionPrefix}_subscription"

    /**
     * The subscription type must NOT be changed
     */
    val subscriptionType: SubscriptionType
}

enum class ClientTopics(override val subscriptionPrefix: String, override val subscriptionType: SubscriptionType) : TopicType {
    RESPONSE("response", SubscriptionType.Exclusive)
}

enum class GlobalTopics(override val subscriptionPrefix: String, override val subscriptionType: SubscriptionType) : TopicType {
    NAMER("global-namer", SubscriptionType.Shared)
}

enum class WorkflowTopics(override val subscriptionPrefix: String, override val subscriptionType: SubscriptionType) : TopicType {
    TAG("workflow-tag", SubscriptionType.Key_Shared),
    ENGINE("workflow-engine", SubscriptionType.Key_Shared),
    DELAY("workflow-$TOPIC_WITH_DELAY", SubscriptionType.Shared)
}

enum class WorkflowTaskTopics(override val subscriptionPrefix: String, override val subscriptionType: SubscriptionType) : TopicType {
    TAG("workflow-task-tag", SubscriptionType.Key_Shared),
    ENGINE("workflow-task-engine", SubscriptionType.Key_Shared),
    DELAY("workflow-task-$TOPIC_WITH_DELAY", SubscriptionType.Shared),
    EXECUTOR("workflow-task-executors", SubscriptionType.Shared)
}

enum class TaskTopics(override val subscriptionPrefix: String, override val subscriptionType: SubscriptionType) : TopicType {
    TAG("task-tag", SubscriptionType.Key_Shared),
    ENGINE("task-engine", SubscriptionType.Key_Shared),
    DELAY("task-$TOPIC_WITH_DELAY", SubscriptionType.Shared),
    EXECUTOR("task-executors", SubscriptionType.Shared)
}
