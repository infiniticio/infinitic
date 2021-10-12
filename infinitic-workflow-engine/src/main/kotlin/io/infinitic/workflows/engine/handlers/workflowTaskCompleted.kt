/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.workflows.engine.handlers

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.CompletedMethod
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.minus
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.InlineTask
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignal
import io.infinitic.common.workflows.data.commands.StartDurationTimer
import io.infinitic.common.workflows.data.commands.StartInstantTimer
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.steps.StepStatus.OngoingFailure
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
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
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.engine.helpers.stepTerminated
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.infinitic.common.workflows.data.commands.DispatchMethod as DispatchMethodCmd
import io.infinitic.common.workflows.data.commands.DispatchTask as DispatchTaskCmd
import io.infinitic.common.workflows.data.commands.DispatchWorkflow as DispatchWorkflowCmd
import io.infinitic.common.workflows.data.commands.SendSignal as SendSignalCmd

internal fun CoroutineScope.workflowTaskCompleted(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    message: TaskCompleted
): MutableList<WorkflowEngineMessage> {
    val workflowTaskOutput = message.taskReturnValue.value() as WorkflowTaskReturnValue

    // retrieve current methodRun
    val methodRun = state.getRunningMethodRun()

    // if current step status was ongoingFailure
    // convert it to a definitive StepStatusFailed
    // as the error has been caught by the workflow
    methodRun.currentStep?.let {
        val oldStatus = it.stepStatus
        if (oldStatus is OngoingFailure) {
            it.stepStatus = Failed(oldStatus.commandId, oldStatus.failureWorkflowTaskIndex)
            methodRun.pastSteps.add(it)
            methodRun.currentStep = null
        }
    }

    // properties updates
    workflowTaskOutput.properties.map {
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
    workflowTaskOutput.newCommands.forEach {
        @Suppress("UNUSED_VARIABLE")
        val o = when (it.command) {
            is DispatchTaskCmd -> dispatchTask(output, methodRun, it, state)
            is DispatchWorkflowCmd -> dispatchWorkflow(output, methodRun, it, state)
            is DispatchMethodCmd -> dispatchMethod(output, methodRun, it, state, bufferedMessages)
            is SendSignalCmd -> sendSignal(output, methodRun, it, state, bufferedMessages)
            is InlineTask -> inlineTask(methodRun, it, state)
            is StartDurationTimer -> startDurationTimer(output, methodRun, it, state)
            is StartInstantTimer -> startInstantTimer(output, methodRun, it, state)
            is ReceiveSignal -> receiveFromChannel(methodRun, it, state)
        }
    }

    // add new step to past steps
    workflowTaskOutput.newStep?.let {
        // checking that currennt step is empty
        if (methodRun.currentStep != null) thisShouldNotHappen("non null current step")
        // set new step
        methodRun.currentStep = PastStep(
            stepPosition = it.stepPosition,
            step = it.step,
            stepHash = it.stepHash,
            stepStatus = it.step.status(),
            workflowTaskIndexAtStart = state.workflowTaskIndex
        )
    }

    // if method is completed for the first time
    if (workflowTaskOutput.methodReturnValue != null && methodRun.methodReturnValue == null) {
        // set methodOutput in state
        methodRun.methodReturnValue = workflowTaskOutput.methodReturnValue

        // send output back to waiting clients
        methodRun.waitingClients.map {
            val workflowCompleted = CompletedMethod(
                emitterName = output.clientName,
                recipientName = it,
                workflowId = state.workflowId,
                methodRunId = methodRun.methodRunId,
                methodReturnValue = methodRun.methodReturnValue!!
            )
            launch { output.sendEventsToClient(workflowCompleted) }
        }

        // tell parent workflow if any
        methodRun.parentWorkflowId?.let {
            val childMethodCompleted = ChildMethodCompleted(
                workflowName = methodRun.parentWorkflowName ?: thisShouldNotHappen(),
                workflowId = it,
                methodRunId = methodRun.parentMethodRunId ?: thisShouldNotHappen(),
                childWorkflowId = state.workflowId,
                childMethodRunId = methodRun.methodRunId,
                childWorkflowReturnValue = workflowTaskOutput.methodReturnValue!!,
                emitterName = output.clientName
            )
            if (it == state.workflowId) {
                // case of method dispatched within same workflow
                bufferedMessages.add(childMethodCompleted)
            } else {
                launch { output.sendToWorkflowEngine(childMethodCompleted) }
            }
        }
    }

    // does previous commands trigger another workflowTask?
    while (state.runningTerminatedCommands.isNotEmpty() && state.runningWorkflowTaskId == null) {
        val commandId = state.runningTerminatedCommands.first()
        val pastCommand = state.getPastCommand(commandId, methodRun)

        if (!stepTerminated(output, state, pastCommand)) {
            // if no additional step can be completed, we can remove this command
            state.runningTerminatedCommands.removeFirst()
        }
    }

    if (methodRun.isTerminated()) state.removeMethodRun(methodRun)

    return bufferedMessages
}

private fun inlineTask(methodRun: MethodRun, newCommand: NewCommand, state: WorkflowState) {
    val status = CommandStatus.Completed(
        returnValue = (newCommand.command as InlineTask).value,
        completionWorkflowTaskIndex = state.workflowTaskIndex
    )

    addPastCommand(methodRun, newCommand, status)
}

private fun CoroutineScope.startDurationTimer(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as StartDurationTimer

    val msg = TimerCompleted(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        methodRunId = methodRun.methodRunId,
        timerId = TimerId.from(newCommand.commandId),
        emitterName = output.clientName
    )

    val diff: MillisDuration = state.runningWorkflowTaskInstant!! - MillisInstant.now()

    launch { output.sendToWorkflowEngineAfter(msg, command.duration - diff) }

    addPastCommand(methodRun, newCommand)
}

private fun CoroutineScope.startInstantTimer(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as StartInstantTimer

    val msg = TimerCompleted(
        workflowName = state.workflowName,
        workflowId = state.workflowId,
        methodRunId = methodRun.methodRunId,
        timerId = TimerId.from(newCommand.commandId),
        emitterName = output.clientName
    )

    launch { output.sendToWorkflowEngineAfter(msg, command.instant - MillisInstant.now()) }

    addPastCommand(methodRun, newCommand)
}

private fun receiveFromChannel(
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as ReceiveSignal

    state.receivingChannels.add(
        ReceivingChannel(
            channelName = command.channelName,
            channelSignalType = command.channelSignalType,
            channelEventFilter = command.channelEventFilter,
            methodRunId = methodRun.methodRunId,
            commandId = newCommand.commandId
        )
    )

    addPastCommand(methodRun, newCommand)
}

private fun CoroutineScope.dispatchTask(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchTaskCmd

    // send task to task engine
    val dispatchTask = DispatchTask(
        taskName = command.taskName,
        taskId = TaskId.from(newCommand.commandId),
        taskOptions = command.taskOptions,
        clientWaiting = false,
        methodName = command.methodName,
        methodParameterTypes = command.methodParameterTypes,
        methodParameters = command.methodParameters,
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        taskTags = command.taskTags,
        taskMeta = command.taskMeta,
        emitterName = ClientName("workflow engine")
    )
    launch { output.sendToTaskEngine(dispatchTask) }

    // add provided tags
    dispatchTask.taskTags.forEach {
        val addTagToTask = AddTagToTask(
            taskName = dispatchTask.taskName,
            taskTag = it,
            taskId = dispatchTask.taskId,
            emitterName = output.clientName
        )
        launch { output.sendToTaskTagEngine(addTagToTask) }
    }

    addPastCommand(methodRun, newCommand)
}

private fun CoroutineScope.dispatchWorkflow(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchWorkflowCmd

    // send task to task engine
    val dispatchWorkflow = DispatchWorkflow(
        workflowName = command.workflowName,
        workflowId = WorkflowId.from(newCommand.commandId),
        methodName = command.methodName,
        methodParameters = command.methodParameters,
        methodParameterTypes = command.methodParameterTypes,
        workflowOptions = state.workflowOptions,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        parentWorkflowName = state.workflowName,
        parentWorkflowId = state.workflowId,
        parentMethodRunId = methodRun.methodRunId,
        clientWaiting = false,
        emitterName = ClientName("workflow engine")
    )
    launch { output.sendToWorkflowEngine(dispatchWorkflow) }

    // add provided tags
    dispatchWorkflow.workflowTags.forEach {
        val addTagToWorkflow = AddTagToWorkflow(
            workflowName = dispatchWorkflow.workflowName,
            workflowTag = it,
            workflowId = dispatchWorkflow.workflowId,
            emitterName = output.clientName
        )
        launch { output.sendToWorkflowTagEngine(addTagToWorkflow) }
    }

    addPastCommand(methodRun, newCommand)
}

private fun getDispatchMethod(
    emitterName: ClientName,
    commandId: CommandId,
    command: DispatchMethodCmd,
    state: WorkflowState
) = DispatchMethod(
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
    emitterName = emitterName
)

private fun CoroutineScope.dispatchMethod(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState,
    bufferedMessages: MutableList<WorkflowEngineMessage>

) {
    val command = newCommand.command as DispatchMethodCmd

    when {
        command.workflowId != null -> {
            val dispatchMethodRun = getDispatchMethod(output.clientName, newCommand.commandId, command, state)

            when (command.workflowId) {
                state.workflowId ->
                    // dispatch method on this workflow
                    bufferedMessages.add(dispatchMethodRun)
                else ->
                    // dispatch method on another workflow
                    launch { output.sendToWorkflowEngine(dispatchMethodRun) }
            }
        }
        command.workflowTag != null -> {
            if (state.workflowTags.contains(command.workflowTag!!)) {
                // dispatch method on this workflow
                bufferedMessages.add(getDispatchMethod(output.clientName, newCommand.commandId, command, state))
            }

            val dispatchMethodByTag = DispatchMethodByTag(
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
                emitterName = output.clientName
            )
            // tag engine must ignore this message if parentWorkflowId has the provided tag
            launch { output.sendToWorkflowTagEngine(dispatchMethodByTag) }
        }
        else -> thisShouldNotHappen()
    }

    addPastCommand(methodRun, newCommand)
}

private fun getSendSignal(
    emitterName: ClientName,
    command: SendSignalCmd
) = SendSignal(
    workflowName = command.workflowName,
    workflowId = command.workflowId!!,
    channelName = command.channelName,
    channelSignalId = ChannelSignalId(),
    channelSignal = command.channelSignal,
    channelSignalTypes = command.channelSignalTypes,
    emitterName = emitterName
)

private fun CoroutineScope.sendSignal(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState,
    bufferedMessages: MutableList<WorkflowEngineMessage>
) {
    val command = newCommand.command as SendSignalCmd

    when {
        command.workflowId != null -> {
            val sendToChannel = getSendSignal(output.clientName, command)

            when (command.workflowId) {
                state.workflowId ->
                    // dispatch signal on current workflow
                    bufferedMessages.add(sendToChannel)
                else ->
                    // dispatch signal on another workflow
                    launch { output.sendToWorkflowEngine(sendToChannel) }
            }
        }
        command.workflowTag != null -> {
            if (state.workflowTags.contains(command.workflowTag!!)) {
                bufferedMessages.add(getSendSignal(output.clientName, command))
            }
            // dispatch signal per tag
            val sendSignalByTag = SendSignalByTag(
                workflowName = command.workflowName,
                workflowTag = command.workflowTag!!,
                channelName = command.channelName,
                channelSignalId = ChannelSignalId(),
                channelSignal = command.channelSignal,
                channelSignalTypes = command.channelSignalTypes,
                emitterWorkflowId = state.workflowId,
                emitterName = output.clientName
            )
            launch { output.sendToWorkflowTagEngine(sendSignalByTag) }
        }
        else -> thisShouldNotHappen()
    }

    addPastCommand(methodRun, newCommand)
}

private fun addPastCommand(
    methodRun: MethodRun,
    newCommand: NewCommand,
    commandStatus: CommandStatus = CommandStatus.Running
): PastCommand {
    val pastCommand = PastCommand(
        commandPosition = newCommand.commandPosition,
        commandType = newCommand.commandType,
        commandId = newCommand.commandId,
        commandHash = newCommand.commandHash,
        commandName = newCommand.commandName,
        commandSimpleName = newCommand.commandSimpleName,
        commandStatus = commandStatus
    )

    methodRun.pastCommands.add(pastCommand)

    return pastCommand
}
