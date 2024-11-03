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
package io.infinitic.common.workflows.engine.commands

import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.workflowId
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.MethodCommandedEvent
import io.infinitic.common.workflows.engine.messages.data.RemoteMethodDispatched
import io.infinitic.common.workflows.engine.messages.data.RemoteMethodDispatchedById
import io.infinitic.common.workflows.engine.messages.data.RemoteMethodDispatchedByTag
import io.infinitic.common.workflows.engine.messages.data.RemoteWorkflowDispatched
import io.infinitic.common.workflows.engine.messages.data.RemoteWorkflowDispatchedByCustomId
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun InfiniticProducer.dispatchRemoteMethod(
  remote: RemoteMethodDispatched,
  requester: Requester,
) = coroutineScope {

  suspend fun getEmitterName() = EmitterName(getName())

  when (remote) {
    // New Workflow
    is RemoteWorkflowDispatched -> {
      // First, we set tags, in case the Engine update them
      // add provided tags, except customId that have been set previously in WorkflowTag
      coroutineScope {
        remote.workflowTags.filter { !it.isCustomId() }.forEach {
          launch {
            val addTagToWorkflow = with(remote) {
              AddTagToWorkflow(
                  workflowName = workflowName,
                  workflowTag = it,
                  workflowId = workflowId,
                  emitterName = getEmitterName(),
                  emittedAt = emittedAt,
              )
            }
            addTagToWorkflow.sendTo(WorkflowTagEngineTopic)
          }
        }
      }

      // send workflow to workflow engine
      val dispatchWorkflow = with(remote) {
        DispatchWorkflow(
            workflowName = workflowName,
            workflowId = workflowId,
            methodName = methodName,
            methodParameters = methodParameters,
            methodParameterTypes = methodParameterTypes,
            workflowTags = workflowTags,
            workflowMeta = workflowMeta,
            requester = requester,
            clientWaiting = false,
            emitterName = getEmitterName(),
            emittedAt = emittedAt,
        )
      }
      launch { dispatchWorkflow.sendTo(WorkflowStateCmdTopic) }

      // send a timeout if needed
      remote.timeout?.let {
        launch {
          val remoteMethodTimedOut = dispatchWorkflow.remoteMethodTimedOut(getEmitterName(), it)
          remoteMethodTimedOut.sendTo(WorkflowStateTimerTopic, it)
        }
      }
    }

    // Workflow dispatched with a custom id
    is RemoteWorkflowDispatchedByCustomId -> {
      // delegate to workflow tag engine
      val dispatchWorkflowByCustomId = with(remote) {
        DispatchWorkflowByCustomId(
            workflowName = workflowName,
            workflowTag = customId,
            workflowId = workflowId,
            methodName = methodName,
            methodParameters = methodParameters,
            methodParameterTypes = methodParameterTypes,
            methodTimeout = timeout,
            workflowTags = workflowTags,
            workflowMeta = workflowMeta,
            requester = requester,
            clientWaiting = false,
            emitterName = getEmitterName(),
            emittedAt = emittedAt,
        )
      }
      dispatchWorkflowByCustomId.sendTo(WorkflowTagEngineTopic)
    }

    // New Method on a workflow targeted by Id
    is RemoteMethodDispatchedById -> {
      val dispatchMethod = with(remote) {
        DispatchMethod(
            workflowName = workflowName,
            workflowId = workflowId,
            workflowMethodName = methodName,
            workflowMethodId = workflowMethodId,
            methodParameters = methodParameters,
            methodParameterTypes = methodParameterTypes,
            requester = requester,
            clientWaiting = false,
            emitterName = getEmitterName(),
            emittedAt = emittedAt,
        )
      }

      launch {
        // Event: Starting remote method
        when (dispatchMethod.workflowId) {
          // if we target the same workflow, the method will be actually be dispatched with
          // the return of the workflowTask, so we just emit MethodCommandedEvent
          requester.workflowId -> {
            // Event: Starting method
            val methodCommandedEvent = with(requester as WorkflowRequester) {
              MethodCommandedEvent(
                  workflowName = workflowName,
                  workflowVersion = workflowVersion,
                  workflowId = workflowId,
                  workflowMethodId = workflowMethodId,
                  methodName = dispatchMethod.workflowMethodName,
                  methodParameters = dispatchMethod.methodParameters,
                  methodParameterTypes = dispatchMethod.methodParameterTypes,
                  requester = requester,
                  emitterName = getEmitterName(),
              )
            }
            methodCommandedEvent.sendTo(WorkflowStateEventTopic)
          }
          // if we target another workflow, the MethodCommandedEvent event
          // will be triggered by WorkflowCmdHandler
          else -> dispatchMethod.sendTo(WorkflowStateCmdTopic)
        }
      }

      // set timeout if any
      // Note: this is valid for both current and other workflows
      // as the timeout is relative to the current workflow
      remote.timeout?.let {
        launch {
          val remoteMethodTimedOut = dispatchMethod.remoteMethodTimedOut(getEmitterName(), it)
          remoteMethodTimedOut.sendTo(WorkflowStateTimerTopic, it)
        }
      }
    }

    // New Method on running workflows defined by tag
    is RemoteMethodDispatchedByTag -> launch {
      val dispatchMethodByTag = with(remote) {
        DispatchMethodByTag(
            workflowName = workflowName,
            workflowTag = workflowTag,
            workflowMethodId = workflowMethodId,
            methodName = methodName,
            methodParameterTypes = methodParameterTypes,
            methodParameters = methodParameters,
            methodTimeout = timeout,
            requester = requester,
            clientWaiting = false,
            emitterName = getEmitterName(),
            emittedAt = emittedAt,
        )
      }
      // Note: tag engine MUST ignore this message for Id = parentWorkflowId
      dispatchMethodByTag.sendTo(WorkflowTagEngineTopic)
    }
  }
}
