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
package io.infinitic.workflows.engine.commands

import io.infinitic.common.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.state.WorkflowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchTaskCmd(
  newCommand: DispatchTaskPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer
) {
  // send task to task executor
  val executeTask: ExecuteTask = with(newCommand.command) {
    ExecuteTask(
        serviceName = serviceName,
        taskId = TaskId.from(newCommand.commandId),
        clientWaiting = false,
        methodName = methodName,
        methodParameterTypes = methodParameterTypes,
        methodParameters = methodParameters,
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        workflowVersion = state.workflowVersion,
        methodRunId = state.runningMethodRunId ?: thisShouldNotHappen(),
        lastError = null,
        taskRetryIndex = TaskRetryIndex(0),
        taskRetrySequence = newCommand.taskRetrySequence,
        taskTags = taskTags,
        taskMeta = taskMeta,
        emitterName = ClientName(producer.name),
    )
  }

  launch { producer.send(executeTask) }

  // add provided tags
  executeTask.taskTags.forEach {
    val addTagToTask = AddTagToTask(
        serviceName = executeTask.serviceName,
        taskTag = it,
        taskId = executeTask.taskId,
        emitterName = ClientName(producer.name),
    )
    launch { producer.send(addTagToTask) }
  }

  // send global task timeout if any
  val timeout = newCommand.command.methodTimeout

  if (timeout != null) {
    val taskTimedOut = with(newCommand.command) {
      TaskTimedOut(
          workflowName = state.workflowName,
          workflowId = state.workflowId,
          methodRunId = state.runningMethodRunId ?: thisShouldNotHappen(),
          taskTimedOutError = TaskTimedOutError(
              serviceName = serviceName,
              taskId = executeTask.taskId,
              methodName = methodName,
          ),
          emitterName = ClientName(producer.name),
      )
    }
    launch { producer.send(taskTimedOut, timeout) }
  }
}
