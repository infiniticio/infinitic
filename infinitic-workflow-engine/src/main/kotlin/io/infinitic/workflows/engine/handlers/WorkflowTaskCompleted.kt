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

import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.DispatchMethodCommand
import io.infinitic.common.workflows.data.commands.DispatchMethodPastCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.data.commands.DispatchWorkflowCommand
import io.infinitic.common.workflows.data.commands.DispatchWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.InlineTaskPastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalPastCommand
import io.infinitic.common.workflows.data.commands.SendSignalCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerPastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.StepStatus.CurrentlyFailed
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowReturnValue
import io.infinitic.common.workflows.engine.messages.ChildMethodCompleted
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.workflows.engine.helpers.dispatchTask
import io.infinitic.workflows.engine.helpers.stepTerminated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.workflowTaskCompleted(
  producer: InfiniticProducer,
  state: WorkflowState,
  message: TaskCompleted
): MutableList<WorkflowEngineMessage> {
  val workflowTaskReturnValue =
      message.taskReturnValue.returnValue.value() as WorkflowTaskReturnValue

  // set workflowVersion the first time
  if (state.workflowVersion == null) {
    state.workflowVersion = workflowTaskReturnValue.workflowVersion
  }

  // retrieve current methodRun
  val methodRun = state.getRunningMethodRun()

  // if current step status was CurrentlyFailed
  // convert it to a definitive StepStatus.Failed
  // as the error has been caught by the workflow
  methodRun.currentStep?.let {
    val oldStatus = it.stepStatus
    if (oldStatus is CurrentlyFailed) {
      it.stepStatus = Failed(oldStatus.deferredFailedError, oldStatus.failureWorkflowTaskIndex)
      methodRun.pastSteps.add(it)
      methodRun.currentStep = null
    }
  }

  // properties updates
  workflowTaskReturnValue.properties.map {
    val hash = it.value.hash()
    if (it.key !in state.currentPropertiesNameHash.keys ||
      hash != state.currentPropertiesNameHash[it.key]) {
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
    @Suppress("UNUSED_VARIABLE")
    val o =
        when (it) {
          is DispatchTaskPastCommand -> dispatchTask(producer, state, it)
          is DispatchWorkflowPastCommand -> dispatchWorkflow(producer, it, state)
          is DispatchMethodPastCommand -> dispatchMethod(producer, it, state, bufferedMessages)
          is SendSignalPastCommand -> sendSignal(producer, it, state, bufferedMessages)
          is InlineTaskPastCommand -> Unit // Nothing to do
          is StartDurationTimerPastCommand -> startDurationTimer(producer, it, state)
          is StartInstantTimerPastCommand -> startInstantTimer(producer, it, state)
          is ReceiveSignalPastCommand -> receiveFromChannel(it, state)
        }
    methodRun.pastCommands.add(it)
  }

  // add new step to past steps
  workflowTaskReturnValue.newStep?.let {
    // checking that current step is empty
    if (methodRun.currentStep != null) thisShouldNotHappen("non null current step")
    // set new step
    methodRun.currentStep =
        PastStep(
            stepPosition = it.stepPosition,
            step = it.step,
            stepHash = it.stepHash,
            stepStatus = it.step.status(),
            workflowTaskIndexAtStart = state.workflowTaskIndex,
        )
  }

  // if method is completed for the first time
  if (workflowTaskReturnValue.methodReturnValue != null && methodRun.methodReturnValue == null) {
    // set methodOutput in state
    methodRun.methodReturnValue = workflowTaskReturnValue.methodReturnValue

    // send output back to waiting clients
    methodRun.waitingClients.forEach {
      val workflowCompleted =
          MethodCompleted(
              recipientName = it,
              workflowId = state.workflowId,
              methodRunId = methodRun.methodRunId,
              methodReturnValue = methodRun.methodReturnValue!!,
              emitterName = ClientName(producer.name),
          )
      launch { producer.send(workflowCompleted) }
    }
    methodRun.waitingClients.clear()

    // tell parent workflow if any
    methodRun.parentWorkflowId?.let {
      val childMethodCompleted =
          ChildMethodCompleted(
              workflowName = methodRun.parentWorkflowName ?: thisShouldNotHappen(),
              workflowId = it,
              methodRunId = methodRun.parentMethodRunId ?: thisShouldNotHappen(),
              childWorkflowReturnValue =
              WorkflowReturnValue(
                  workflowId = state.workflowId,
                  methodRunId = methodRun.methodRunId,
                  returnValue = workflowTaskReturnValue.methodReturnValue!!,
              ),
              emitterName = ClientName(producer.name),
          )
      if (it == state.workflowId) {
        // case of method dispatched within same workflow
        bufferedMessages.add(childMethodCompleted)
      } else {
        launch { producer.send(childMethodCompleted) }
      }
    }
  }

  // does previous commands trigger another workflowTask?
  while (state.runningTerminatedCommands.isNotEmpty() && state.runningWorkflowTaskId == null) {
    val commandId = state.runningTerminatedCommands.first()
    val pastCommand = state.getPastCommand(commandId, methodRun)

    if (!stepTerminated(producer, state, pastCommand)) {
      // if no additional step can be completed, we can remove this command
      state.runningTerminatedCommands.removeFirst()
    }
  }

  if (methodRun.isTerminated()) state.removeMethodRun(methodRun)

  return bufferedMessages
}

private fun CoroutineScope.startDurationTimer(
  producer: InfiniticProducer,
  newCommand: StartDurationTimerPastCommand,
  state: WorkflowState
) {
  val command: StartDurationTimerCommand = newCommand.command

  val msg = TimerCompleted(
      workflowName = state.workflowName,
      workflowId = state.workflowId,
      methodRunId = state.runningMethodRunId ?: thisShouldNotHappen(),
      timerId = TimerId.from(newCommand.commandId),
      emitterName = ClientName(producer.name),
  )
  // The duration is offset by the time spent in the workflow task
  val diff = state.runningWorkflowTaskInstant!! - MillisInstant.now()

  launch { producer.send(msg, command.duration + diff) }
}

private fun CoroutineScope.startInstantTimer(
  producer: InfiniticProducer,
  newCommand: StartInstantTimerPastCommand,
  state: WorkflowState
) {
  val command: StartInstantTimerCommand = newCommand.command

  val msg =
      TimerCompleted(
          workflowName = state.workflowName,
          workflowId = state.workflowId,
          methodRunId = state.runningMethodRunId ?: thisShouldNotHappen(),
          timerId = TimerId.from(newCommand.commandId),
          emitterName = ClientName(producer.name),
      )

  launch { producer.send(msg, command.instant - MillisInstant.now()) }
}

private fun receiveFromChannel(newCommand: ReceiveSignalPastCommand, state: WorkflowState) {
  val command: ReceiveSignalCommand = newCommand.command

  state.receivingChannels.add(
      ReceivingChannel(
          channelName = command.channelName,
          channelType = command.channelType,
          channelFilter = command.channelFilter,
          methodRunId = state.runningMethodRunId!!,
          commandId = newCommand.commandId,
          receivedSignalLimit = command.receivedSignalLimit,
      ),
  )
}

private fun CoroutineScope.dispatchWorkflow(
  producer: InfiniticProducer,
  newCommand: DispatchWorkflowPastCommand,
  state: WorkflowState
) {
  val command: DispatchWorkflowCommand = newCommand.command

  val customIds = command.workflowTags.filter { it.isCustomId() }

  when (customIds.size) {
    // no customId tag provided
    0 -> {
      // send workflow to workflow engine
      val dispatchWorkflow =
          DispatchWorkflow(
              workflowName = command.workflowName,
              workflowId = WorkflowId.from(newCommand.commandId),
              methodName = command.methodName,
              methodParameters = command.methodParameters,
              methodParameterTypes = command.methodParameterTypes,
              workflowTags = command.workflowTags,
              workflowMeta = command.workflowMeta,
              parentWorkflowName = state.workflowName,
              parentWorkflowId = state.workflowId,
              parentMethodRunId = state.runningMethodRunId,
              clientWaiting = false,
              emitterName = ClientName(producer.name),
          )
      launch { producer.send(dispatchWorkflow) }

      // add provided tags
      dispatchWorkflow.workflowTags.forEach {
        val addTagToWorkflow =
            AddTagToWorkflow(
                workflowName = dispatchWorkflow.workflowName,
                workflowTag = it,
                workflowId = dispatchWorkflow.workflowId,
                emitterName = ClientName(producer.name),
            )
        launch { producer.send(addTagToWorkflow) }
      }
    }

    1 -> {
      // dispatch workflow message with customId tag
      val dispatchWorkflowByCustomId =
          DispatchWorkflowByCustomId(
              workflowName = command.workflowName,
              workflowTag = customIds.first(),
              workflowId = WorkflowId.from(newCommand.commandId),
              methodName = command.methodName,
              methodParameters = command.methodParameters,
              methodParameterTypes = command.methodParameterTypes,
              workflowTags = command.workflowTags,
              workflowMeta = command.workflowMeta,
              parentWorkflowName = state.workflowName,
              parentWorkflowId = state.workflowId,
              parentMethodRunId = state.runningMethodRunId,
              clientWaiting = false,
              emitterName = ClientName(producer.name),
          )

      launch { producer.send(dispatchWorkflowByCustomId) }
    }
    // this must be excluded from workflow task
    else -> thisShouldNotHappen()
  }
}

private fun getDispatchMethod(
  emitterName: ClientName,
  commandId: CommandId,
  command: DispatchMethodCommand,
  state: WorkflowState
) =
    DispatchMethod(
        workflowName = command.workflowName,
        workflowId = command.workflowId!!,
        methodRunId = MethodRunId.from(commandId),
        methodName = command.methodName,
        methodParameters = command.methodParameters,
        methodParameterTypes = command.methodParameterTypes,
        parentWorkflowId = state.workflowId,
        parentWorkflowName = state.workflowName,
        parentMethodRunId = state.runningMethodRunId,
        clientWaiting = false,
        emitterName = emitterName,
    )

private fun CoroutineScope.dispatchMethod(
  producer: InfiniticProducer,
  newCommand: DispatchMethodPastCommand,
  state: WorkflowState,
  bufferedMessages: MutableList<WorkflowEngineMessage>
) {
  val command: DispatchMethodCommand = newCommand.command

  when {
    command.workflowId != null -> {
      val dispatchMethodRun =
          getDispatchMethod(ClientName(producer.name), newCommand.commandId, command, state)

      when (command.workflowId) {
        state.workflowId ->
          // dispatch method on this workflow
          bufferedMessages.add(dispatchMethodRun)

        else ->
          // dispatch method on another workflow
          launch { producer.send(dispatchMethodRun) }
      }
    }

    command.workflowTag != null -> {
      if (state.workflowTags.contains(command.workflowTag!!)) {
        // dispatch method on this workflow
        bufferedMessages.add(
            getDispatchMethod(ClientName(producer.name), newCommand.commandId, command, state),
        )
      }

      val dispatchMethodByTag =
          DispatchMethodByTag(
              workflowName = command.workflowName,
              workflowTag = command.workflowTag!!,
              parentWorkflowId = state.workflowId,
              parentWorkflowName = state.workflowName,
              parentMethodRunId = state.runningMethodRunId,
              methodRunId = MethodRunId.from(newCommand.commandId),
              methodName = command.methodName,
              methodParameterTypes = command.methodParameterTypes,
              methodParameters = command.methodParameters,
              clientWaiting = false,
              emitterName = ClientName(producer.name),
          )
      // tag engine must ignore this message if parentWorkflowId has the provided tag
      launch { producer.send(dispatchMethodByTag) }
    }

    else -> thisShouldNotHappen()
  }
}

private fun getSendSignal(
  emitterName: ClientName,
  commandId: CommandId,
  command: SendSignalCommand
) =
    SendSignal(
        workflowName = command.workflowName,
        workflowId = command.workflowId!!,
        channelName = command.channelName,
        signalId = SignalId.from(commandId),
        signalData = command.signalData,
        channelTypes = command.channelTypes,
        emitterName = emitterName,
    )

private fun CoroutineScope.sendSignal(
  producer: InfiniticProducer,
  newCommand: SendSignalPastCommand,
  state: WorkflowState,
  bufferedMessages: MutableList<WorkflowEngineMessage>
) {
  val command: SendSignalCommand = newCommand.command

  when {
    command.workflowId != null -> {
      val sendToChannel = getSendSignal(ClientName(producer.name), newCommand.commandId, command)

      when (command.workflowId) {
        state.workflowId ->
          // dispatch signal on current workflow
          bufferedMessages.add(sendToChannel)

        else ->
          // dispatch signal on another workflow
          launch { producer.send(sendToChannel) }
      }
    }

    command.workflowTag != null -> {
      if (state.workflowTags.contains(command.workflowTag!!)) {
        val sendToChannel = getSendSignal(ClientName(producer.name), newCommand.commandId, command)
        bufferedMessages.add(sendToChannel)
      }
      // dispatch signal per tag
      val sendSignalByTag =
          SendSignalByTag(
              workflowName = command.workflowName,
              workflowTag = command.workflowTag!!,
              channelName = command.channelName,
              signalId = SignalId(),
              signalData = command.signalData,
              channelTypes = command.channelTypes,
              emitterWorkflowId = state.workflowId,
              emitterName = ClientName(producer.name),
          )
      launch { producer.send(sendSignalByTag) }
    }

    else -> thisShouldNotHappen()
  }
}
