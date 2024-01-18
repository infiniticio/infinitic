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
package io.infinitic.workflows.engine.handlers

import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.topics.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.engine.messages.RetryTasks
import io.infinitic.common.workflows.engine.messages.TaskTimedOut
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.DeferredStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.retryTasks(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: RetryTasks
) {
  val taskId = message.taskId?.run { CommandId.from(this) }
  val taskStatus = when (message.taskStatus) {
    DeferredStatus.ONGOING -> CommandStatus.Ongoing::class
    DeferredStatus.UNKNOWN -> CommandStatus.Unknown::class
    DeferredStatus.CANCELED -> CommandStatus.Canceled::class
    DeferredStatus.FAILED -> CommandStatus.Failed::class
    DeferredStatus.TIMED_OUT -> CommandStatus.TimedOut::class
    DeferredStatus.COMPLETED -> CommandStatus.Completed::class
    null -> null
  }
  val serviceName = message.serviceName

  // for all method runs
  state.workflowMethods.forEach { workflowMethod ->
    // for all past tasks
    workflowMethod.pastCommands
        .filterIsInstance<DispatchTaskPastCommand>()
        .filter {
          (taskId == null || it.commandId == taskId) &&
              (taskStatus == null || it.commandStatus::class == taskStatus) &&
              (serviceName == null || it.command.serviceName == serviceName)
        }
        // dispatch a new sequence of those task
        .forEach { dispatchTaskPastCommand ->
          dispatchTaskPastCommand.taskRetrySequence += 1
          reDispatchTaskCmd(dispatchTaskPastCommand, state, producer)
          dispatchTaskPastCommand.commandStatus = CommandStatus.Ongoing
        }
  }
}

private fun CoroutineScope.reDispatchTaskCmd(
  pastCommand: DispatchTaskPastCommand,
  state: WorkflowState,
  producer: InfiniticProducer
) {
  val emitterName = EmitterName(producer.name)

  // send task to task executor
  val executeTask: ExecuteTask = with(pastCommand.command) {
    ExecuteTask(
        serviceName = serviceName,
        taskId = TaskId.from(pastCommand.commandId),
        emitterName = emitterName,
        taskRetrySequence = pastCommand.taskRetrySequence,
        taskRetryIndex = TaskRetryIndex(0),
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        workflowMethodId = state.runningWorkflowMethodId ?: thisShouldNotHappen(),
        taskTags = taskTags,
        taskMeta = taskMeta,
        clientWaiting = false,
        methodName = methodName,
        methodParameterTypes = methodParameterTypes,
        methodParameters = methodParameters,
        lastError = null,
        workflowVersion = state.workflowVersion,
    )
  }

  launch { producer.sendToServiceExecutor(executeTask) }

  // add provided tags
  executeTask.taskTags.forEach {
    val addTagToTask = AddTagToTask(
        serviceName = executeTask.serviceName,
        taskTag = it,
        taskId = executeTask.taskId,
        emitterName = emitterName,
    )
    launch { producer.sendToServiceTag(addTagToTask) }
  }

  // send global task timeout if any
  pastCommand.command.methodTimeout?.let {
    val taskTimedOut = with(pastCommand.command) {
      TaskTimedOut(
          taskTimedOutError = TaskTimedOutError(
              serviceName = serviceName,
              taskId = executeTask.taskId,
              methodName = methodName,
          ),
          workflowName = state.workflowName,
          workflowId = state.workflowId,
          workflowMethodId = state.runningWorkflowMethodId ?: thisShouldNotHappen(),
          emitterName = emitterName,
          emittedAt = state.runningWorkflowTaskInstant,
      )
    }
    launch { with(producer) { taskTimedOut.sendTo(DelayedWorkflowEngineTopic, it) } }
  }
}

