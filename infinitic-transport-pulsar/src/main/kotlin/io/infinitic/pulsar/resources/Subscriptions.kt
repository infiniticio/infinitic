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
package io.infinitic.pulsar.resources

import io.infinitic.common.topics.ServiceTagTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.topics.WorkflowCmdTopic
import io.infinitic.common.topics.WorkflowEngineTopic
import io.infinitic.common.topics.WorkflowTagTopic
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType

interface Subscription {
  fun type(topic: Topic<*>): SubscriptionType
  fun name(topic: Topic<*>): String
  fun nameDLQ(topic: Topic<*>): String
  fun initialPosition(topic: Topic<*>): SubscriptionInitialPosition
}

object MainSubscription : Subscription {
  override fun type(topic: Topic<*>) = when (topic) {
    ServiceTagTopic,
    WorkflowTagTopic,
    WorkflowCmdTopic,
    WorkflowEngineTopic -> SubscriptionType.Key_Shared

    else -> SubscriptionType.Shared
  }

  override fun name(topic: Topic<*>) = "${topic.prefix()}-subscription"

  override fun nameDLQ(topic: Topic<*>) = "${name(topic)}-dlq"

  override fun initialPosition(topic: Topic<*>) = SubscriptionInitialPosition.Earliest
}

object ListenerSubscription : Subscription {
  override fun type(topic: Topic<*>) = SubscriptionType.Shared

  override fun name(topic: Topic<*>) = "${topic.prefix()}-listener-subscription"

  override fun nameDLQ(topic: Topic<*>) = "${name(topic)}-dlq"

  override fun initialPosition(topic: Topic<*>) = SubscriptionInitialPosition.Latest
}
