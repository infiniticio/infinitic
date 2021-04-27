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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.minus
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.commands.CommandCompleted
import io.infinitic.common.workflows.data.commands.CommandOngoing
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflows.data.commands.EndAsync
import io.infinitic.common.workflows.data.commands.EndInlineTask
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.ReceiveInChannel
import io.infinitic.common.workflows.data.commands.SendToChannel
import io.infinitic.common.workflows.data.commands.StartAsync
import io.infinitic.common.workflows.data.commands.StartDurationTimer
import io.infinitic.common.workflows.data.commands.StartInlineTask
import io.infinitic.common.workflows.data.commands.StartInstantTimer
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.StepStatusFailed
import io.infinitic.common.workflows.data.steps.StepStatusOngoing
import io.infinitic.common.workflows.data.steps.StepStatusOngoingFailure
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflowTasks.plus
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.helpers.cleanMethodRunIfNeeded
import io.infinitic.workflows.engine.helpers.commandTerminated
import io.infinitic.workflows.engine.helpers.dispatchWorkflowTask
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import io.infinitic.common.clients.messages.WorkflowCompleted as WorkflowCompletedInClient
import io.infinitic.common.workflows.data.commands.DispatchTask as DispatchTaskInWorkflow

internal suspend fun workflowTaskCompleted(
    workflowEngineOutput: WorkflowEngineOutput,
    state: WorkflowState,
    msg: TaskCompleted
) {
    val workflowTaskOutput = msg.taskReturnValue.get() as WorkflowTaskReturnValue

    // retrieve current methodRun
    val methodRun = state.getRunningMethodRun()

    // if current step status was ongoingFailure
    // convert it to a definitive StepStatusFailed
    // as the error has been caught by the workflow
    methodRun.getStepByPosition(state.runningMethodRunPosition!!)
        ?.run {
            val status = stepStatus
            if (status is StepStatusOngoingFailure) {
                stepStatus = StepStatusFailed(status.error, status.failureWorkflowTaskIndex)
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

    // add new commands to past commands
    workflowTaskOutput.newCommands.forEach {
        // making when an expression to force exhaustiveness
        val o = when (it.command) {
            is DispatchTaskInWorkflow -> dispatchTask(workflowEngineOutput, methodRun, it, state)
            is DispatchChildWorkflow -> dispatchChildWorkflow(workflowEngineOutput, methodRun, it, state)
            is StartAsync -> startAsync(methodRun, it, state)
            is EndAsync -> endAsync(workflowEngineOutput, methodRun, it, state)
            is StartInlineTask -> startInlineTask(methodRun, it)
            is EndInlineTask -> endInlineTask(methodRun, it, state)
            is StartDurationTimer -> startDurationTimer(workflowEngineOutput, methodRun, it, state)
            is StartInstantTimer -> startInstantTimer(workflowEngineOutput, methodRun, it, state)
            is ReceiveInChannel -> receiveFromChannel(methodRun, it, state)
            is SendToChannel -> TODO()
        }
    }

    // add new steps to past steps
    workflowTaskOutput.newSteps.map {
        methodRun.pastSteps.add(
            PastStep(
                stepPosition = it.stepPosition,
                step = it.step,
                stepHash = it.stepHash,
                stepStatus = StepStatusOngoing
            )
        )
    }

    // if method is completed for the first time
    if (workflowTaskOutput.methodReturnValue != null && methodRun.methodReturnValue == null) {
        // set methodOutput in state
        methodRun.methodReturnValue = workflowTaskOutput.methodReturnValue

        // send output back to waiting clients
        methodRun.waitingClients.map {
            val workflowCompleted = WorkflowCompletedInClient(
                clientName = it,
                workflowId = state.workflowId,
                workflowReturnValue = methodRun.methodReturnValue!!
            )
            workflowEngineOutput.sendEventsToClient(workflowCompleted)
        }

        // tell parent workflow if any
        methodRun.parentWorkflowId?.let {
            val childWorkflowCompleted = ChildWorkflowCompleted(
                workflowId = it,
                workflowName = state.workflowName,
                methodRunId = methodRun.parentMethodRunId!!,
                childWorkflowId = state.workflowId,
                childWorkflowReturnValue = workflowTaskOutput.methodReturnValue!!
            )
            workflowEngineOutput.sendToWorkflowEngine(childWorkflowCompleted)
        }
    }

    // does previous commands trigger another workflowTask?
    while (state.bufferedCommands.isNotEmpty() && state.runningWorkflowTaskId == null) {
        val commandId = state.bufferedCommands.first()
        val pastCommand = methodRun.getPastCommand(commandId)

        when (pastCommand.commandType) {
            CommandType.START_ASYNC -> {
                // update pastCommand with a copy (!) of current properties and anticipated workflowTaskIndex
                pastCommand.propertiesNameHashAtStart = state.currentPropertiesNameHash.toMap()
                pastCommand.workflowTaskIndexAtStart = state.workflowTaskIndex + 1
                // dispatch a new workflowTask
                dispatchWorkflowTask(
                    workflowEngineOutput,
                    state,
                    methodRun,
                    pastCommand.commandPosition
                )
                // removes this command
                state.bufferedCommands.removeAt(0)
            }
            CommandType.DISPATCH_CHILD_WORKFLOW,
            CommandType.DISPATCH_TASK,
            CommandType.START_DURATION_TIMER,
            CommandType.START_INSTANT_TIMER,
            CommandType.RECEIVE_IN_CHANNEL,
            CommandType.END_ASYNC -> {
                // note: pastSteps is naturally ordered by time => the first branch completed is the earliest step
                when (val pastStep = methodRun.pastSteps.find { it.isTerminatedBy(pastCommand) }) {
                    null -> state.bufferedCommands.removeAt(0)
                    else -> {
                        // update pastStep with current properties and anticipated workflowTaskIndex
                        pastStep.propertiesNameHashAtTermination = state.currentPropertiesNameHash.toMap()
                        pastStep.workflowTaskIndexAtTermination = state.workflowTaskIndex + 1
                        // dispatch a new workflowTask
                        dispatchWorkflowTask(
                            workflowEngineOutput,
                            state,
                            methodRun,
                            pastStep.stepPosition
                        )
                        // state.bufferedCommands is untouched as we could have another pastStep solved by this command
                    }
                }
            }
            else -> throw RuntimeException("This should not happen: unmanaged ${pastCommand.commandType} type in  state.bufferedCommands")
        }
    }

    // if everything is completed in methodRun then filter state
    cleanMethodRunIfNeeded(methodRun, state)
}

private fun startAsync(methodRun: MethodRun, newCommand: NewCommand, state: WorkflowState) {
    val pastCommand = addPastCommand(methodRun, newCommand)

    state.bufferedCommands.add(pastCommand.commandId)
}

private suspend fun endAsync(
    workflowEngineOutput: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as EndAsync
    // look for previous Start Async command
    val pastStartAsync = methodRun.pastCommands.first {
        it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_ASYNC
    }

    val commandStatus = CommandCompleted(
        command.asyncReturnValue,
        state.workflowTaskIndex
    )

    commandTerminated(
        workflowEngineOutput,
        state,
        methodRun.methodRunId,
        pastStartAsync.commandId,
        commandStatus
    )
}

private fun startInlineTask(methodRun: MethodRun, newCommand: NewCommand) {
    addPastCommand(methodRun, newCommand)
}

private fun endInlineTask(methodRun: MethodRun, newCommand: NewCommand, state: WorkflowState) {
    val command = newCommand.command as EndInlineTask
    // look for previous StartInlineTask command
    val pastStartInlineTask = methodRun.pastCommands.first {
        it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_INLINE_TASK
    }
    // past command completed
    pastStartInlineTask.commandStatus = CommandCompleted(
        returnValue = command.inlineTaskReturnValue,
        completionWorkflowTaskIndex = state.workflowTaskIndex
    )
}

private suspend fun startDurationTimer(
    workflowEngineOutput: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as StartDurationTimer

    val msg = TimerCompleted(
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        timerId = TimerId(newCommand.commandId.id)
    )

    val diff: MillisDuration = state.runningWorkflowTaskInstant!! - MillisInstant.now()

    workflowEngineOutput.sendToWorkflowEngineAfter(msg, command.duration - diff)

    addPastCommand(methodRun, newCommand)
}

private suspend fun startInstantTimer(
    workflowEngineOutput: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as StartInstantTimer

    val msg = TimerCompleted(
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        timerId = TimerId(newCommand.commandId.id)
    )

    workflowEngineOutput.sendToWorkflowEngineAfter(msg, command.instant - MillisInstant.now())

    addPastCommand(methodRun, newCommand)
}

private fun receiveFromChannel(
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as ReceiveInChannel

    state.receivingChannels.add(
        ReceivingChannel(
            channelName = command.channelName,
            channelEventType = command.channelEventType,
            channelEventFilter = command.channelEventFilter,
            methodRunId = methodRun.methodRunId,
            commandId = newCommand.commandId
        )
    )

    addPastCommand(methodRun, newCommand)
}

private suspend fun dispatchTask(
    workflowEngineOutput: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchTaskInWorkflow
    // send task to task engine
    val msg = DispatchTask(
        clientName = ClientName("workflow engine"),
        clientWaiting = false,
        taskId = TaskId(newCommand.commandId.id),
        taskName = command.taskName,
        methodName = command.methodName,
        methodParameterTypes = command.methodParameterTypes,
        methodParameters = command.methodParameters,
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        taskTags = command.taskTags,
        taskMeta = command.taskMeta,
        taskOptions = command.taskOptions
    )
    workflowEngineOutput.sendToTaskEngine(msg)

    addPastCommand(methodRun, newCommand)
}

private suspend fun dispatchChildWorkflow(
    workflowEngineOutput: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchChildWorkflow
    // send task to task engine
    val msg = DispatchWorkflow(
        clientName = ClientName("workflow engine"),
        clientWaiting = false,
        workflowId = WorkflowId(newCommand.commandId.id),
        parentWorkflowId = state.workflowId,
        parentWorkflowName = state.workflowName,
        parentMethodRunId = methodRun.methodRunId,
        workflowName = command.childWorkflowName,
        methodName = command.childMethodName,
        methodParameterTypes = command.childMethodParameterTypes,
        methodParameters = command.childMethodParameters,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        workflowOptions = state.workflowOptions
    )
    workflowEngineOutput.sendToWorkflowEngine(msg)

    addPastCommand(methodRun, newCommand)
}

private fun addPastCommand(
    methodRun: MethodRun,
    newCommand: NewCommand
): PastCommand {
    val pastCommand = PastCommand(
        commandPosition = newCommand.commandPosition,
        commandType = newCommand.commandType,
        commandId = newCommand.commandId,
        commandHash = newCommand.commandHash,
        commandName = newCommand.commandName,
        commandSimpleName = newCommand.commandSimpleName,
        commandStatus = CommandOngoing
    )

    methodRun.pastCommands.add(pastCommand)

    return pastCommand
}
