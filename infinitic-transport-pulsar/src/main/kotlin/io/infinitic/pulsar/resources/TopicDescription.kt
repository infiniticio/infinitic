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

import org.apache.pulsar.client.api.SubscriptionType

/** must NOT be changed (would change the name of the subscriptions of delayed messages) */
const val TOPIC_WITH_DELAY = "delay"

sealed interface TopicDescription {

  /**
   * The subscriptionPrefix must NOT be changed (if subscription name is changed, all messages will
   * appear as not acknowledged to a new worker)
   */
  val subscriptionPrefix: String

  /**
   * The subscriptionName must NOT be changed (if subscription name is changed, all messages will
   * appear as not acknowledged to a new worker)
   */
  val subscriptionName: String
    get() = "$subscriptionPrefix-subscription"

  /**
   * The subscriptionNameDlq must NOT be changed (if subscription name is changed, all messages will
   * appear as not acknowledged to a new worker)
   */
  val subscriptionNameDlq: String
    get() = "$subscriptionName-dlq"

  val subscriptionType: SubscriptionType

  val isPartitioned: Boolean

  val isDelayed: Boolean

  val hasDeadLetter: Boolean
}

enum class ClientTopicDescription : TopicDescription {
  RESPONSE {
    override val subscriptionPrefix = "response"
    override val subscriptionType = SubscriptionType.Exclusive
    override val isPartitioned = false
    override val isDelayed = false
    override val hasDeadLetter = false
  }
}

enum class GlobalTopicDescription : TopicDescription {
  NAMER {
    override val subscriptionPrefix = "namer"
    override val subscriptionType = SubscriptionType.Shared
    override val isPartitioned = false
    override val isDelayed = false
    override val hasDeadLetter = false
  }
}

enum class WorkflowTopicDescription : TopicDescription {
  TAG {
    override val subscriptionPrefix = "workflow-tag"
    override val subscriptionType = SubscriptionType.Key_Shared
    override val isPartitioned = true
    override val isDelayed = false
    override val hasDeadLetter = true
  },
  CMD {
    override val subscriptionPrefix = "workflow-cmd"
    override val subscriptionType = SubscriptionType.Key_Shared
    override val isPartitioned = true
    override val isDelayed = false
    override val hasDeadLetter = true
  },
  ENGINE {
    override val subscriptionPrefix = "workflow-engine"
    override val subscriptionType = SubscriptionType.Key_Shared
    override val isPartitioned = true
    override val isDelayed = false
    override val hasDeadLetter = true
  },
  ENGINE_DELAYED {
    override val subscriptionPrefix = "workflow-$TOPIC_WITH_DELAY"
    override val subscriptionType = SubscriptionType.Shared
    override val isPartitioned = true
    override val isDelayed = true
    override val hasDeadLetter = true
  },
  EXECUTOR {
    override val subscriptionPrefix = "workflow-task-executor"
    override val subscriptionType = SubscriptionType.Shared
    override val isPartitioned = true
    override val isDelayed = false
    override val hasDeadLetter = true
  }
}

enum class ServiceTopicDescription : TopicDescription {
  TAG {
    override val subscriptionPrefix = "task-tag"
    override val subscriptionType = SubscriptionType.Key_Shared
    override val isPartitioned = true
    override val isDelayed = false
    override val hasDeadLetter = true
  },
  EXECUTOR {
    override val subscriptionPrefix = "task-executor"
    override val subscriptionType = SubscriptionType.Shared
    override val isPartitioned = true
    override val isDelayed = false
    override val hasDeadLetter = true
  }
}

