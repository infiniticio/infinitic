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
package io.infinitic.common.workflows.engine.messages.data

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.cloudEvents.CHANNEL_NAME
import io.infinitic.cloudEvents.EMITTED_AT
import io.infinitic.cloudEvents.METHOD_ARGS
import io.infinitic.cloudEvents.METHOD_ID
import io.infinitic.cloudEvents.METHOD_NAME
import io.infinitic.cloudEvents.SERVICE_NAME
import io.infinitic.cloudEvents.SIGNAL_DATA
import io.infinitic.cloudEvents.SIGNAL_ID
import io.infinitic.cloudEvents.TASK_ARGS
import io.infinitic.cloudEvents.TASK_ID
import io.infinitic.cloudEvents.TASK_META
import io.infinitic.cloudEvents.TASK_NAME
import io.infinitic.cloudEvents.TASK_RETRY_SEQUENCE
import io.infinitic.cloudEvents.TASK_TAGS
import io.infinitic.cloudEvents.TIMEOUT
import io.infinitic.cloudEvents.TIMER_DURATION
import io.infinitic.cloudEvents.TIMER_ID
import io.infinitic.cloudEvents.TIMER_INSTANT
import io.infinitic.cloudEvents.WORKFLOW_ID
import io.infinitic.cloudEvents.WORKFLOW_META
import io.infinitic.cloudEvents.WORKFLOW_NAME
import io.infinitic.cloudEvents.WORKFLOW_TAG
import io.infinitic.cloudEvents.WORKFLOW_TAGS
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodArgs
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.utils.JsonAble
import io.infinitic.common.utils.toJson
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.isWorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class SignalDiscarded(
  val signalId: SignalId
) : JsonAble {
  override fun toJson() = JsonObject(
      mapOf(
          SIGNAL_ID to signalId.toJson(),
      ),
  )
}


@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class SignalReceived(
  val signalId: SignalId
) : JsonAble {
  override fun toJson() = JsonObject(
      mapOf(
          SIGNAL_ID to signalId.toJson(),
      ),
  )
}

@Serializable
sealed interface RemoteMethodDispatched : JsonAble {
  val workflowName: WorkflowName
  val workflowMethodName: MethodName
  val workflowMethodId: WorkflowMethodId
  val methodName: MethodName
  val methodParameters: MethodArgs
  val methodParameterTypes: MethodParameterTypes?
  val timeout: MillisDuration?
  val emittedAt: MillisInstant
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class RemoteWorkflowDispatched(
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  val workflowId: WorkflowId,
  override val workflowName: WorkflowName,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val methodName: MethodName,
  override val methodParameters: MethodArgs,
  override val methodParameterTypes: MethodParameterTypes?,
  override val timeout: MillisDuration?,
  override val emittedAt: MillisInstant
) : RemoteMethodDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_ID to workflowId.toJson(),
          METHOD_NAME to methodName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_ARGS to methodParameters.toJson(),
          WORKFLOW_TAGS to workflowTags.toJson(),
          WORKFLOW_META to workflowMeta.toJson(),
          TIMEOUT to timeout.toJson(),
      ),
  )
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class RemoteWorkflowDispatchedByCustomId(
  val customId: WorkflowTag,
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  val workflowId: WorkflowId,
  override val workflowName: WorkflowName,
  override val workflowMethodName: MethodName,
  override val workflowMethodId: WorkflowMethodId,
  override val methodName: MethodName,
  override val methodParameters: MethodArgs,
  override val methodParameterTypes: MethodParameterTypes?,
  override val timeout: MillisDuration?,
  override val emittedAt: MillisInstant
) : RemoteMethodDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_ID to workflowId.toJson(),
          METHOD_NAME to methodName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_ARGS to methodParameters.toJson(),
          WORKFLOW_TAGS to workflowTags.toJson(),
          WORKFLOW_META to workflowMeta.toJson(),
          TIMEOUT to timeout.toJson(),
      ),
  )
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class RemoteMethodDispatchedById(
  val workflowId: WorkflowId,
  override val workflowName: WorkflowName,
  override val workflowMethodId: WorkflowMethodId,
  override val workflowMethodName: MethodName,
  override val methodName: MethodName,
  override val methodParameters: MethodArgs,
  override val methodParameterTypes: MethodParameterTypes?,
  override val timeout: MillisDuration?,
  override val emittedAt: MillisInstant
) : RemoteMethodDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_ID to workflowId.toJson(),
          METHOD_NAME to methodName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          METHOD_ARGS to methodParameters.toJson(),
          TIMEOUT to timeout.toJson(),
      ),
  )
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class RemoteMethodDispatchedByTag(
  val workflowTag: WorkflowTag,
  override val workflowName: WorkflowName,
  override val workflowMethodId: WorkflowMethodId,
  override val workflowMethodName: MethodName,
  override val methodName: MethodName,
  override val methodParameters: MethodArgs,
  override val methodParameterTypes: MethodParameterTypes?,
  override val timeout: MillisDuration?,
  override val emittedAt: MillisInstant
) : RemoteMethodDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          WORKFLOW_NAME to workflowName.toJson(),
          WORKFLOW_TAG to workflowTag.toJson(),
          METHOD_NAME to methodName.toJson(),
          METHOD_ID to workflowMethodId.toJson(),
          TIMEOUT to timeout.toJson(),
      ),
  )
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class TaskDispatched(
  val serviceName: ServiceName,
  val taskId: TaskId,
  val taskRetrySequence: TaskRetrySequence,
  val methodName: MethodName,
  val methodParameterTypes: MethodParameterTypes?,
  val methodParameters: MethodArgs,
  val taskTags: Set<TaskTag>,
  val taskMeta: TaskMeta,
  val timeoutInstant: MillisInstant?,
) : JsonAble {
  override fun toJson() = JsonObject(
      when (serviceName.isWorkflowTask()) {
        true -> mapOf(
            TASK_ARGS to methodParameters.toJson(),
            TASK_ID to taskId.toJson(),
            TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
        )

        false -> mapOf(
            SERVICE_NAME to serviceName.toJson(),
            TASK_NAME to methodName.toJson(),
            TASK_ARGS to methodParameters.toJson(),
            TASK_ID to taskId.toJson(),
            TASK_META to taskMeta.toJson(),
            TASK_TAGS to taskTags.toJson(),
            TASK_RETRY_SEQUENCE to taskRetrySequence.toJson(),
        )
      },
  )
}

@Serializable
sealed interface RemoteSignalDispatched : JsonAble {
  val signalId: SignalId
  val signalData: SignalData
  val channelName: ChannelName
  val channelTypes: Set<ChannelType>
  val workflowName: WorkflowName
  val emittedAt: MillisInstant
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class RemoteSignalDispatchedById(
  val workflowId: WorkflowId,
  override val signalId: SignalId,
  override val signalData: SignalData,
  override val channelName: ChannelName,
  override val channelTypes: Set<ChannelType>,
  override val workflowName: WorkflowName,
  override val emittedAt: MillisInstant
) : RemoteSignalDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          SIGNAL_ID to signalId.toJson(),
          SIGNAL_DATA to signalData.toJson(),
          CHANNEL_NAME to channelName.toJson(),
          WORKFLOW_ID to workflowId.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
      ),
  )
}


@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class RemoteSignalDispatchedByTag(
  val workflowTag: WorkflowTag,
  override val signalId: SignalId,
  override val signalData: SignalData,
  override val channelName: ChannelName,
  override val channelTypes: Set<ChannelType>,
  override val workflowName: WorkflowName,
  override val emittedAt: MillisInstant
) : RemoteSignalDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          SIGNAL_ID to signalId.toJson(),
          SIGNAL_DATA to signalData.toJson(),
          CHANNEL_NAME to channelName.toJson(),
          WORKFLOW_TAG to workflowTag.toJson(),
          WORKFLOW_NAME to workflowName.toJson(),
      ),
  )
}

@Serializable
sealed interface TimerDispatched : JsonAble {
  val timerId: TimerId
  val emittedAt: MillisInstant
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class DurationTimerDispatched(
  override val timerId: TimerId,
  override val emittedAt: MillisInstant,
  val duration: MillisDuration,
) : TimerDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          TIMER_ID to timerId.toJson(),
          TIMER_DURATION to duration.toJson(),
          EMITTED_AT to emittedAt.toJson(),
      ),
  )
}

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
data class InstantTimerDispatched(
  override val timerId: TimerId,
  override val emittedAt: MillisInstant,
  val timerInstant: MillisInstant
) : TimerDispatched {
  override fun toJson() = JsonObject(
      mapOf(
          TIMER_ID to timerId.toJson(),
          TIMER_INSTANT to timerInstant.toJson(),
          EMITTED_AT to emittedAt.toJson(),
      ),
  )
}
