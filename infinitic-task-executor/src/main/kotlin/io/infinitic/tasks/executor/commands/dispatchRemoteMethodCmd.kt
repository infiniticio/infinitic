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
package io.infinitic.tasks.executor.commands

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.workflows.data.commands.DispatchNewMethodCommand
import io.infinitic.common.workflows.data.commands.DispatchNewMethodPastCommand
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchRemoteMethodCmd(
  current: WorkflowRequester,
  pastCommand: DispatchNewMethodPastCommand,
  workflowTaskInstant: MillisInstant,
  producer: InfiniticProducer,
) {
  val emitterName = EmitterName(producer.name)
  val command: DispatchNewMethodCommand = pastCommand.command
  val workflowMethodId = WorkflowMethodId.from(pastCommand.commandId)

  when {
    command.workflowId != null -> {
      val dispatchMethod = DispatchMethod(
          workflowName = command.workflowName,
          workflowId = command.workflowId!!,
          workflowMethodName = command.methodName,
          workflowMethodId = workflowMethodId,
          methodParameters = command.methodParameters,
          methodParameterTypes = command.methodParameterTypes,
          requester = current,
          clientWaiting = false,
          emitterName = emitterName,
          emittedAt = workflowTaskInstant,
      )

      launch {
        // Event: Starting remote method
        with(producer) {
          dispatchMethod.remoteMethodDispatchedEvent(emitterName).sendTo(WorkflowEventsTopic)
        }
        when (command.workflowId == current.workflowId) {
          // if we target the same workflow, the method will be actually be dispatched with
          // the return of the workflowTask, so we just emit MethodCommandedEvent
          true -> {
            // Event: Starting method
            val methodCommandedEvent = MethodCommandedEvent(
                workflowName = current.workflowName,
                workflowVersion = current.workflowVersion,
                workflowId = current.workflowId,
                workflowMethodId = dispatchMethod.workflowMethodId,
                methodName = dispatchMethod.workflowMethodName,
                methodParameters = dispatchMethod.methodParameters,
                methodParameterTypes = dispatchMethod.methodParameterTypes,
                requester = current,
                emitterName = emitterName,
            )
            with(producer) { methodCommandedEvent.sendTo(WorkflowEventsTopic) }
          }
          // if we target another workflow, the MethodCommandedEvent event
          // will be triggered by WorkflowCmdHandler
          false -> with(producer) { dispatchMethod.sendTo(WorkflowCmdTopic) }
        }
      }

      // set timeout if any
      // Note: this is valid for both current and other workflows
      // as the timeout is relative to the current workflow
      command.methodTimeout?.let {
        launch {
          val remoteMethodTimedOut =
              dispatchMethod.remoteMethodTimedOut(emitterName, it)
          with(producer) { remoteMethodTimedOut.sendTo(DelayedWorkflowEngineTopic, it) }
        }
      }
    }

    command.workflowTag != null -> {
      val dispatchMethodByTag = DispatchMethodByTag(
          workflowName = command.workflowName,
          workflowTag = command.workflowTag!!,
          workflowMethodId = workflowMethodId,
          methodName = command.methodName,
          methodParameterTypes = command.methodParameterTypes,
          methodParameters = command.methodParameters,
          methodTimeout = command.methodTimeout,
          requester = current,
          clientWaiting = false,
          emitterName = emitterName,
          emittedAt = workflowTaskInstant,
      )
      // Note: tag engine MUST ignore this message for Id = parentWorkflowId
      launch {
        with(producer) { dispatchMethodByTag.sendTo(WorkflowTagTopic) }
      }
    }
  }
}
