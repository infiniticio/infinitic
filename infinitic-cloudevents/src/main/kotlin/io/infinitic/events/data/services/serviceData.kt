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

import io.infinitic.cloudEvents.ERROR
import io.infinitic.cloudEvents.INFINITIC_VERSION
import io.infinitic.cloudEvents.OUTPUT
import io.infinitic.cloudEvents.REQUESTER
import io.infinitic.cloudEvents.SERVICE_NAME
import io.infinitic.cloudEvents.TASK_ARGS
import io.infinitic.cloudEvents.TASK_META
import io.infinitic.cloudEvents.TASK_NAME
import io.infinitic.cloudEvents.TASK_RETRY_DELAY
import io.infinitic.cloudEvents.TASK_RETRY_INDEX
import io.infinitic.cloudEvents.TASK_RETRY_SEQUENCE
import io.infinitic.cloudEvents.TASK_TAGS
import io.infinitic.cloudEvents.WORKER_NAME
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.utils.toJson
import kotlinx.serialization.json.JsonObject

fun ServiceExecutorMessage.toJson() = when (this) {
  is ExecuteTask -> JsonObject(
      mapOf(
          TASK_ARGS to methodArgs.toJson(),
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          SERVICE_NAME to serviceName.toJson(),
          TASK_NAME to methodName.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          REQUESTER to requester.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}

fun ServiceExecutorEventMessage.toJson(): JsonObject = when (this) {

  is TaskStartedEvent -> JsonObject(
      mapOf(
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          SERVICE_NAME to serviceName.toJson(),
          TASK_NAME to methodName.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskRetriedEvent -> JsonObject(
      mapOf(
          ERROR to failure.toJson(),
          TASK_RETRY_DELAY to taskRetryDelay.toJson(),
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          SERVICE_NAME to serviceName.toJson(),
          TASK_NAME to methodName.toJson(),
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
          SERVICE_NAME to serviceName.toJson(),
          TASK_NAME to methodName.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )

  is TaskCompletedEvent -> JsonObject(
      mapOf(
          OUTPUT to returnValue.toJson(),
          TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
          TASK_RETRY_INDEX to taskRetryIndex.toJson(),
          SERVICE_NAME to serviceName.toJson(),
          TASK_NAME to methodName.toJson(),
          TASK_META to taskMeta.toJson(),
          TASK_TAGS to taskTags.toJson(),
          WORKER_NAME to emitterName.toJson(),
          INFINITIC_VERSION to version.toJson(),
      ),
  )
}
