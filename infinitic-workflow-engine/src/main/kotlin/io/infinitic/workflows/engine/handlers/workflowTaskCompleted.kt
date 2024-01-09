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
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.commands.DispatchMethodOnRunningWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.data.commands.InlineTaskPastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalPastCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerPastCommand
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.StepStatus.CurrentlyFailed
import io.infinitic.common.workflows.data.steps.StepStatus.CurrentlyTimedOut
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.steps.StepStatus.TimedOut
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.engine.events.WorkflowMethodCompletedEvent
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.commands.dispatchMethodOnRunningWorkflowCmd
import io.infinitic.workflows.engine.commands.receiveSignalCmd
import io.infinitic.workflows.engine.commands.sendSignalCmd
import io.infinitic.workflows.engine.commands.startDurationTimerCmd
import io.infinitic.workflows.engine.commands.startInstantTimerCmq
import io.infinitic.workflows.engine.helpers.stepTerminated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.workflowTaskCompleted(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: TaskCompleted
) {
  val emitterName = EmitterName(producer.name)

  val workflowTaskReturnValue =
      message.taskReturnValue.returnValue.value() as WorkflowTaskReturnValue

  // set workflowVersion
  when (state.workflowVersion) {
    null -> state.workflowVersion = workflowTaskReturnValue.workflowVersion
    workflowTaskReturnValue.workflowVersion -> Unit
    else -> thisShouldNotHappen()
  }

  // retrieve current methodRun
  val workflowMethod = state.getRunningWorkflowMethod()

  // if current step status was CurrentlyFailed / CurrentlyTimedOut
  // convert it to a definitive StepStatus.Failed / StepStatus.TimedOut
  // as the error has been caught by the workflow
  workflowMethod.currentStep?.let {
    val oldStatus = it.stepStatus
    if (oldStatus is CurrentlyFailed) {
      it.stepStatus = Failed(oldStatus.deferredFailedError, oldStatus.failureWorkflowTaskIndex)
      workflowMethod.pastSteps.add(it)
      workflowMethod.currentStep = null
    }
    if (oldStatus is CurrentlyTimedOut) {
      it.stepStatus = TimedOut(oldStatus.deferredTimedOutError, oldStatus.timeoutWorkflowTaskIndex)
      workflowMethod.pastSteps.add(it)
      workflowMethod.currentStep = null
    }
  }

  // properties updates
  workflowTaskReturnValue.properties.map {
    val hash = it.value.hash()
    if (it.key !in state.currentPropertiesNameHash.keys || hash != state.currentPropertiesNameHash[it.key]) {
      // new or updated property
      state.currentPropertiesNameHash[it.key] = hash
    }
    if (hash !in state.propertiesHashValue.keys) {
      state.propertiesHashValue[hash] = it.value
    }
  }

  val bufferedMessages = mutableListOf<WorkflowEngineMessage>()

  // add new commands to past commands
  workflowTaskReturnValue.newCommands.forEach {
    when (it) {
      is DispatchTaskPastCommand -> Unit // dispatchTaskCmd(it, state, producer)
      is DispatchNewWorkflowPastCommand -> Unit // dispatchNewWorkflowCmd(it, state, producer)
      is DispatchMethodOnRunningWorkflowPastCommand -> dispatchMethodOnRunningWorkflowCmd(
          it,
          state,
          producer,
          bufferedMessages,
      )

      is SendSignalPastCommand -> sendSignalCmd(it, state, producer, bufferedMessages)
      is InlineTaskPastCommand -> Unit // Nothing to do
      is StartDurationTimerPastCommand -> startDurationTimerCmd(it, state, producer)
      is StartInstantTimerPastCommand -> startInstantTimerCmq(it, state, producer)
      is ReceiveSignalPastCommand -> receiveSignalCmd(it, state)
    }
    workflowMethod.pastCommands.add(it)
  }

  // add new step to past steps
  workflowTaskReturnValue.newStep?.let {
    // checking that current step is empty
    if (workflowMethod.currentStep != null) thisShouldNotHappen("non null current step")
    // set new step
    workflowMethod.currentStep = PastStep(
        stepPosition = it.stepPosition,
        step = it.step,
        stepHash = it.stepHash,
        stepStatus = it.step.status(),
        workflowTaskIndexAtStart = state.workflowTaskIndex,
    )
  }

  // if method is completed for the first time
  if (workflowTaskReturnValue.methodReturnValue != null && workflowMethod.methodReturnValue == null) {
    // set methodOutput in state
    workflowMethod.methodReturnValue = workflowTaskReturnValue.methodReturnValue

    val workflowMethodCompletedEvent = WorkflowMethodCompletedEvent(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        waitingClients = workflowMethod.waitingClients,
        workflowMethodId = workflowMethod.workflowMethodId,
        parentWorkflowId = workflowMethod.parentWorkflowId,
        parentWorkflowName = workflowMethod.parentWorkflowName,
        parentWorkflowMethodId = workflowMethod.parentWorkflowMethodId,
        parentClientName = workflowMethod.parentClientName,
        returnValue = workflowMethod.methodReturnValue!!,
        emitterName = emitterName,
    )

    launch { producer.sendToWorkflowEvents(workflowMethodCompletedEvent) }


    // tell itself if needed
    if (workflowMethodCompletedEvent.isItsOwnParent()) {
      bufferedMessages.add(workflowMethodCompletedEvent.getEventForParentWorkflow()!!)
    }
  }

  // does previous commands trigger another workflowTask?
  while (state.runningTerminatedCommands.isNotEmpty() && state.runningWorkflowTaskId == null) {
    val commandId = state.runningTerminatedCommands.first()
    val pastCommand = state.getPastCommand(commandId, workflowMethod)

    if (!stepTerminated(producer, state, pastCommand)) {
      // if no additional step can be completed, we can remove this command
      state.runningTerminatedCommands.removeFirst()
    }
  }

  if (workflowMethod.isTerminated()) state.removeWorkflowMethod(workflowMethod)

  // add fake messages at the top of the messagesBuffer list
  state.messagesBuffer.addAll(0, bufferedMessages)
}
