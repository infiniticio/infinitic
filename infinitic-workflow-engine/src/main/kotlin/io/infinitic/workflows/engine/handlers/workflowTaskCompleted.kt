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
import io.infinitic.common.tasks.tags.messages.AddTaskTag
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.commands.CommandStatus
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
import io.infinitic.common.workflows.data.steps.StepStatus.Failed
import io.infinitic.common.workflows.data.steps.StepStatus.OngoingFailure
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.common.workflows.data.workflowTasks.plus
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.TaskCompleted
import io.infinitic.common.workflows.engine.messages.TimerCompleted
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.workflows.engine.helpers.dispatchWorkflowTask
import io.infinitic.workflows.engine.helpers.stepTerminated
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.infinitic.common.clients.messages.MethodCompleted as MethodCompletedInClient
import io.infinitic.common.workflows.data.commands.DispatchTask as DispatchTaskInWorkflow

internal fun CoroutineScope.workflowTaskCompleted(
    workflowEngineOutput: WorkflowEngineOutput,
    state: WorkflowState,
    message: TaskCompleted
) {
    val workflowTaskOutput = message.taskReturnValue.get() as WorkflowTaskReturnValue

    // retrieve current methodRun
    val methodRun = state.getRunningMethodRun()

    // if current step status was ongoingFailure
    // convert it to a definitive StepStatusFailed
    // as the error has been caught by the workflow
    methodRun.getStepByPosition(state.runningMethodRunPosition!!)
        ?.run {
            val status = stepStatus
            if (status is OngoingFailure) {
                stepStatus = Failed(status.commandId, status.failureWorkflowTaskIndex)
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
        @Suppress("UNUSED_VARIABLE")
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
                stepStatus = it.step.status()
            )
        )
    }

    // if method is completed for the first time
    if (workflowTaskOutput.methodReturnValue != null && methodRun.methodReturnValue == null) {
        // set methodOutput in state
        methodRun.methodReturnValue = workflowTaskOutput.methodReturnValue

        // send output back to waiting clients
        methodRun.waitingClients.map {
            val workflowCompleted = MethodCompletedInClient(
                clientName = it,
                workflowId = state.workflowId,
                methodRunId = methodRun.methodRunId,
                workflowReturnValue = methodRun.methodReturnValue!!
            )
            launch { workflowEngineOutput.sendEventsToClient(workflowCompleted) }
        }

        // tell parent workflow if any
        methodRun.parentWorkflowId?.let {
            val childWorkflowCompleted = ChildWorkflowCompleted(
                workflowId = it,
                workflowName = methodRun.parentWorkflowName!!,
                methodRunId = methodRun.parentMethodRunId!!,
                childWorkflowId = state.workflowId,
                childWorkflowReturnValue = workflowTaskOutput.methodReturnValue!!
            )
            launch { workflowEngineOutput.sendToWorkflowEngine(childWorkflowCompleted) }
        }
    }

    // does previous commands trigger another workflowTask?
    while (state.runningMethodRunBufferedCommands.isNotEmpty() && state.runningWorkflowTaskId == null) {
        val commandId = state.runningMethodRunBufferedCommands.first()
        val pastCommand = methodRun.getPastCommand(commandId)

        if (pastCommand.commandType == CommandType.START_ASYNC && ! pastCommand.isTerminated()) {
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
            state.runningMethodRunBufferedCommands.removeFirst()
        } else {
            if (!stepTerminated(
                    workflowEngineOutput,
                    state,
                    methodRun,
                    pastCommand
                )
            ) {
                // no step is completed, we can remove this command
                state.runningMethodRunBufferedCommands.removeFirst()
            }
        }
    }

    if (methodRun.isTerminated()) state.removeMethodRun(methodRun)
}

private fun startAsync(methodRun: MethodRun, newCommand: NewCommand, state: WorkflowState) {
    val pastCommand = addPastCommand(methodRun, newCommand)

    state.runningMethodRunBufferedCommands.add(pastCommand.commandId)
}

private fun CoroutineScope.endAsync(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as EndAsync

    // look for previous Start Async command
    val pastCommand = methodRun.pastCommands.first {
        it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_ASYNC
    }

    // do nothing if this command is already terminated (i.e. canceled or completed, failed is transient)
    if (pastCommand.isTerminated()) return

    // update command status
    pastCommand.commandStatus = CommandStatus.Completed(command.asyncReturnValue, state.workflowTaskIndex)

    if (stepTerminated(
            output,
            state,
            methodRun,
            pastCommand
        )
    ) {
        // keep this command as we could have another pastStep solved by it
        state.runningMethodRunBufferedCommands.add(pastCommand.commandId)
    }
}

private fun startInlineTask(methodRun: MethodRun, newCommand: NewCommand) {
    addPastCommand(methodRun, newCommand)
}

private fun endInlineTask(methodRun: MethodRun, newCommand: NewCommand, state: WorkflowState) {
    val command = newCommand.command as EndInlineTask
    // look for previous StartInlineTask command
    val pastCommand = methodRun.pastCommands.first {
        it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_INLINE_TASK
    }
    // past command completed
    pastCommand.commandStatus = CommandStatus.Completed(
        returnValue = command.inlineTaskReturnValue,
        completionWorkflowTaskIndex = state.workflowTaskIndex
    )
}

private fun CoroutineScope.startDurationTimer(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as StartDurationTimer

    val msg = TimerCompleted(
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        timerId = TimerId.from(newCommand.commandId)
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
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        timerId = TimerId.from(newCommand.commandId)
    )

    launch { output.sendToWorkflowEngineAfter(msg, command.instant - MillisInstant.now()) }

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
    val command = newCommand.command as DispatchTaskInWorkflow

    // send task to task engine
    val dispatchTask = DispatchTask(
        clientName = ClientName("workflow engine"),
        clientWaiting = false,
        taskId = TaskId.from(newCommand.commandId),
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
    launch { output.sendToTaskEngine(dispatchTask) }

    // add provided tags
    dispatchTask.taskTags.forEach {
        val addTaskTag = AddTaskTag(
            taskTag = it,
            taskName = dispatchTask.taskName,
            taskId = dispatchTask.taskId
        )
        launch { output.sendToTaskTagEngine(addTaskTag) }
    }

    addPastCommand(methodRun, newCommand)
}

private fun CoroutineScope.dispatchChildWorkflow(
    output: WorkflowEngineOutput,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchChildWorkflow

    // send task to task engine
    val dispatchWorkflow = DispatchWorkflow(
        workflowId = WorkflowId.from(newCommand.commandId),
        workflowName = command.childWorkflowName,
        clientName = ClientName("workflow engine"),
        clientWaiting = false,
        parentWorkflowId = state.workflowId,
        parentWorkflowName = state.workflowName,
        parentMethodRunId = methodRun.methodRunId,
        methodName = command.childMethodName,
        methodParameterTypes = command.childMethodParameterTypes,
        methodParameters = command.childMethodParameters,
        workflowTags = state.workflowTags,
        workflowOptions = state.workflowOptions,
        workflowMeta = state.workflowMeta
    )
    launch { output.sendToWorkflowEngine(dispatchWorkflow) }

    // add provided tags
    dispatchWorkflow.workflowTags.forEach {
        val addWorkflowTag = AddWorkflowTag(
            workflowTag = it,
            workflowName = dispatchWorkflow.workflowName,
            workflowId = dispatchWorkflow.workflowId
        )
        launch { output.sendToWorkflowTagEngine(addWorkflowTag) }
    }

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
        commandStatus = CommandStatus.Running
    )

    methodRun.pastCommands.add(pastCommand)

    return pastCommand
}
