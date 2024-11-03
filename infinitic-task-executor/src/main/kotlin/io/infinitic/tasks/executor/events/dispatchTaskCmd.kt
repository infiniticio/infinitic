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
package io.infinitic.tasks.executor.events

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.engine.commands.dispatchTask
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.data.TaskDispatched
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchTaskCmd(
  current: WorkflowRequester,
  pastCommand: DispatchTaskPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer
) = launch {
  val emitterName = EmitterName(producer.getProducerName())
  val dispatchTaskCommand = pastCommand.command

  // Description of the dispatched task
  val taskDispatched = with(dispatchTaskCommand) {
    TaskDispatched(
        taskId = TaskId.from(pastCommand.commandId),
        methodName = methodName,
        methodParameterTypes = methodParameterTypes,
        methodParameters = methodParameters,
        serviceName = serviceName,
        taskMeta = taskMeta,
        taskTags = taskTags,
        taskRetrySequence = TaskRetrySequence(),
        timeoutInstant = methodTimeout?.let { it + workflowTaskInstant },
    )
  }
  // Dispatching the task
  with(producer) { dispatchTask(taskDispatched, current) }

  // Description of the workflow event
  val taskDispatchedEvent = TaskDispatchedEvent(
      taskDispatched = taskDispatched,
      workflowName = current.workflowName,
      workflowId = current.workflowId,
      workflowVersion = current.workflowVersion,
      workflowMethodName = current.workflowMethodName,
      workflowMethodId = current.workflowMethodId,
      emitterName = emitterName,
  )
  // Dispatching the workflow event
  with(producer) { taskDispatchedEvent.sendTo(WorkflowStateEventTopic) }
}
