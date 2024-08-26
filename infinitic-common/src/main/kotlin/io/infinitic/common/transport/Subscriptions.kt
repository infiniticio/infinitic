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
package io.infinitic.common.transport

import io.infinitic.common.messages.Message

sealed class Subscription<S : Message> {
  abstract val topic: Topic<S>
  abstract val withKey: Boolean
}

data class MainSubscription<S : Message>(override val topic: Topic<S>) : Subscription<S>() {
  init {
    if (topic.acceptDelayed) require(!withKey) { "Keyed subscription are forbidden for topics accepting delayed messages" }
  }

  override val withKey
    get() = when (topic) {
      WorkflowTagEngineTopic,
      WorkflowStateCmdTopic,
      WorkflowStateEngineTopic,
      WorkflowExecutorTopic,
      ServiceTagEngineTopic -> true

      else -> false
    }
}

data class EventListenerSubscription<S : Message>(
  override val topic: Topic<S>,
  val name: String?
) : Subscription<S>() {
  override val withKey = false
}


