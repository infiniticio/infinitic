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

import io.infinitic.common.transport.ListenerSubscription
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType

val Subscription<*>.type
  get() = when (withKey) {
    true -> SubscriptionType.Key_Shared
    false -> SubscriptionType.Shared
  }

val Subscription<*>.name
  get() = when (this) {
    is MainSubscription -> "${topic.prefix()}-subscription"
    is ListenerSubscription -> "${topic.prefix()}-listener-subscription"
  }

val Subscription<*>.nameDLQ
  get() = when (this) {
    is MainSubscription -> "${topic.prefix()}-dlq-subscription"
    is ListenerSubscription -> "${topic.prefix()}-dlq-listener-subscription"
  }

val Subscription<*>.initialPosition
  get() = when (this) {
    is MainSubscription -> SubscriptionInitialPosition.Earliest
    is ListenerSubscription -> SubscriptionInitialPosition.Latest
  }

