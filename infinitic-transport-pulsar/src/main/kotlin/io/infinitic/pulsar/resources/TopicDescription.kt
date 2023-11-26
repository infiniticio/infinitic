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

enum class ClientTopicDescription(
  override val subscriptionPrefix: String,
  override val subscriptionType: SubscriptionType,
  override val isPartitioned: Boolean,
  override val isDelayed: Boolean,
  override val hasDeadLetter: Boolean
) : TopicDescription {
  RESPONSE(
      subscriptionPrefix = "response",
      subscriptionType = SubscriptionType.Exclusive,
      isPartitioned = false,
      isDelayed = false,
      hasDeadLetter = false,
  ),
}

enum class GlobalTopicDescription(
  override val subscriptionPrefix: String,
  override val subscriptionType: SubscriptionType,
  override val isPartitioned: Boolean,
  override val isDelayed: Boolean,
  override val hasDeadLetter: Boolean
) : TopicDescription {
  NAMER(
      subscriptionPrefix = "namer",
      subscriptionType = SubscriptionType.Shared,
      isPartitioned = false,
      isDelayed = false,
      hasDeadLetter = false,
  )
}

enum class WorkflowTopicDescription(
  override val subscriptionPrefix: String,
  override val subscriptionType: SubscriptionType,
  override val isPartitioned: Boolean,
  override val isDelayed: Boolean,
  override val hasDeadLetter: Boolean
) : TopicDescription {
  TAG(
      subscriptionPrefix = "workflow-tag",
      subscriptionType = SubscriptionType.Key_Shared,
      isPartitioned = true,
      isDelayed = false,
      hasDeadLetter = true,
  ),
  ENGINE(
      subscriptionPrefix = "workflow-engine",
      subscriptionType = SubscriptionType.Key_Shared,
      isPartitioned = true,
      isDelayed = false,
      hasDeadLetter = true,
  ),
  ENGINE_DELAYED(
      subscriptionPrefix = "workflow-$TOPIC_WITH_DELAY",
      subscriptionType = SubscriptionType.Shared,
      isPartitioned = true,
      isDelayed = true,
      hasDeadLetter = true,
  ),
  EXECUTOR(
      subscriptionPrefix = "workflow-task-executor",
      subscriptionType = SubscriptionType.Shared,
      isPartitioned = true,
      isDelayed = false,
      hasDeadLetter = true,
  )
}

enum class ServiceTopicDescription(
  override val subscriptionPrefix: String,
  override val subscriptionType: SubscriptionType,
  override val isPartitioned: Boolean,
  override val isDelayed: Boolean,
  override val hasDeadLetter: Boolean

) : TopicDescription {
  TAG(
      subscriptionPrefix = "task-tag",
      subscriptionType = SubscriptionType.Key_Shared,
      isPartitioned = true,
      isDelayed = false,
      hasDeadLetter = true,
  ),
  EXECUTOR(
      subscriptionPrefix = "task-executor",
      subscriptionType = SubscriptionType.Shared,
      isPartitioned = true,
      isDelayed = false,
      hasDeadLetter = true,
  )
}

