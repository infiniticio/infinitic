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

package io.infinitic.events.data.services

import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.utils.toJson
import io.infinitic.events.properties.ERROR
import io.infinitic.events.properties.INFINITIC_VERSION
import io.infinitic.events.properties.RESULT
import io.infinitic.events.properties.TASK_META
import io.infinitic.events.properties.TASK_RETRY_DELAY
import io.infinitic.events.properties.TASK_RETRY_INDEX
import io.infinitic.events.properties.TASK_RETRY_SEQUENCE
import io.infinitic.events.properties.TASK_TAGS
import io.infinitic.events.properties.WORKER_NAME
import io.infinitic.events.types.TASK_COMPLETED
import io.infinitic.events.types.TASK_FAILED
import io.infinitic.events.types.TASK_RETRIED
import io.infinitic.events.types.TASK_STARTED
import kotlinx.serialization.json.JsonObject

fun ServiceEventMessage.serviceType(): String = when (this) {
  is TaskCompletedEvent -> TASK_COMPLETED
  is TaskFailedEvent -> TASK_FAILED
  is TaskRetriedEvent -> TASK_RETRIED
  is TaskStartedEvent -> TASK_STARTED
}

fun ServiceEventMessage.toServiceJson(): JsonObject = when (this) {

  is TaskStartedEvent -> JsonObject(
      mapOf(
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskRetriedEvent -> JsonObject(
      mapOf(
          ERROR to lastError.toJson(),
          TASK_RETRY_DELAY to taskRetryDelay.toJson(),
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskFailedEvent -> JsonObject(
      mapOf(
          ERROR to executionError.toJson(),
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskCompletedEvent -> JsonObject(
      mapOf(
          RESULT to returnValue.toJson(),
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}
