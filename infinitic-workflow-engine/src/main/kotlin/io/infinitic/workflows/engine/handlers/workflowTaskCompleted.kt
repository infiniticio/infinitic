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

import io.infinitic.common.data.interfaces.plus
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflows.data.commands.DispatchReceiver
import io.infinitic.common.workflows.data.commands.DispatchTimer
import io.infinitic.common.workflows.data.commands.EndAsync
import io.infinitic.common.workflows.data.commands.EndInlineTask
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.StartAsync
import io.infinitic.common.workflows.data.commands.StartInlineTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.StepStatusOngoing
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowCompleted
import io.infinitic.common.workflows.engine.messages.WorkflowTaskCompleted
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.helpers.cleanMethodRunIfNeeded
import io.infinitic.workflows.engine.helpers.dispatchWorkflowTask
import io.infinitic.workflows.engine.helpers.getMethodRun
import io.infinitic.workflows.engine.helpers.getPastCommand
import io.infinitic.workflows.engine.helpers.jobCompleted
import io.infinitic.common.workflows.data.commands.DispatchTask as DispatchTaskInWorkflow

suspend fun workflowTaskCompleted(
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskEngine: SendToTaskEngine,
    state: WorkflowState,
    msg: WorkflowTaskCompleted
) {
    // remove currentWorkflowTaskId
    state.runningWorkflowTaskId = null

    val workflowTaskOutput = msg.workflowTaskOutput
    val methodRun = getMethodRun(state, workflowTaskOutput.methodRunId)

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
    workflowTaskOutput.newCommands.map {
        when (it.command) {
            is DispatchTaskInWorkflow -> dispatchTask(sendToTaskEngine, methodRun, it, state)
            is DispatchChildWorkflow -> dispatchChildWorkflow(sendToWorkflowEngine, methodRun, it, state)
            is StartAsync -> startAsync(methodRun, it, state)
            is EndAsync -> endAsync(sendToWorkflowEngine, sendToTaskEngine, methodRun, it, state)
            is StartInlineTask -> startInlineTask(methodRun, it)
            is EndInlineTask -> endInlineTask(methodRun, it, state)
            is DispatchTimer -> TODO()
            is DispatchReceiver -> TODO()
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
    if (workflowTaskOutput.methodOutput != null && methodRun.methodOutput == null) {
        // set methodOutput in state
        methodRun.methodOutput = workflowTaskOutput.methodOutput

        // if this is the main method, it means the workflow is completed
        if (methodRun.isMain) {
            sendToWorkflowEngine(
                WorkflowCompleted(
                    workflowId = state.workflowId,
                    workflowOutput = methodRun.methodOutput!!
                ),
                0F
            )
        }

        // tell parent workflow if any
        methodRun.parentWorkflowId?.let {
            sendToWorkflowEngine(
                ChildWorkflowCompleted(
                    workflowId = it,
                    methodRunId = methodRun.parentMethodRunId!!,
                    childWorkflowId = state.workflowId,
                    childWorkflowOutput = workflowTaskOutput.methodOutput!!
                ),
                0F
            )
        }
    }

    // does previous commands trigger another workflowTask?
    if (state.bufferedCommands.isNotEmpty()) {
        val commandId = state.bufferedCommands.first()
        val pastCommand = getPastCommand(methodRun, commandId)

        when (pastCommand.commandType) {
            CommandType.START_ASYNC -> {
                // update pastCommand with a copy (!) of current properties and anticipated workflowTaskIndex
                pastCommand.propertiesNameHashAtStart = state.currentPropertiesNameHash.toMap()
                pastCommand.workflowTaskIndexAtStart = state.workflowTaskIndex + 1
                // dispatch a new workflowTask
                dispatchWorkflowTask(
                    sendToWorkflowEngine,
                    sendToTaskEngine,
                    state,
                    methodRun,
                    pastCommand.commandPosition
                )
                // removes this command
                state.bufferedCommands.removeAt(0)
            }
            CommandType.DISPATCH_CHILD_WORKFLOW,
            CommandType.DISPATCH_TASK,
            CommandType.DISPATCH_RECEIVER,
            CommandType.DISPATCH_TIMER,
            CommandType.END_ASYNC -> {
                // note: pastSteps is naturally ordered by time
                // => the first branch completed is the earliest step
                val pastStep = methodRun.pastSteps.find { it.isTerminatedBy(pastCommand) }
                if (pastStep == null) {
                    // remove it
                    state.bufferedCommands.removeAt(0)
                } else {
                    // update pastStep with current properties and anticipated workflowTaskIndex
                    pastStep.propertiesNameHashAtTermination = state.currentPropertiesNameHash.toMap()
                    pastStep.workflowTaskIndexAtTermination = state.workflowTaskIndex + 1
                    // dispatch a new workflowTask
                    dispatchWorkflowTask(
                        sendToWorkflowEngine,
                        sendToTaskEngine,
                        state,
                        methodRun,
                        pastStep.stepPosition
                    )
                    // state.bufferedWorkflowTasks is untouched as we could have another pastStep solved by this command
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
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskEngine: SendToTaskEngine,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as EndAsync
    // look for previous Start Async command
    val pastStartAsync = methodRun.pastCommands.first {
        it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_ASYNC
    }

    jobCompleted(
        sendToWorkflowEngine,
        sendToTaskEngine,
        state,
        methodRun.methodRunId,
        pastStartAsync.commandId,
        command.asyncOutput
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
    pastStartInlineTask.commandStatus = CommandStatusCompleted(
        command.inlineTaskOutput,
        state.workflowTaskIndex
    )
}

private suspend fun dispatchTask(
    sendToTaskEngine: SendToTaskEngine,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchTaskInWorkflow
    // send task to task engine
    val msg = DispatchTask(
        taskId = TaskId("${newCommand.commandId}"),
        taskName = command.taskName,
        methodName = command.methodName,
        methodParameterTypes = command.methodParameterTypes,
        methodInput = command.methodInput,
        workflowId = state.workflowId,
        methodRunId = methodRun.methodRunId,
        taskMeta = command.taskMeta
    )
    sendToTaskEngine(msg, 0F)

    addPastCommand(methodRun, newCommand)
}

private suspend fun dispatchChildWorkflow(
    sendToWorkflowEngine: SendToWorkflowEngine,
    methodRun: MethodRun,
    newCommand: NewCommand,
    state: WorkflowState
) {
    val command = newCommand.command as DispatchChildWorkflow
    // send task to task engine
    val msg = DispatchWorkflow(
        workflowId = WorkflowId("${newCommand.commandId}"),
        parentWorkflowId = state.workflowId,
        parentMethodRunId = methodRun.methodRunId,
        workflowName = command.childWorkflowName,
        methodName = command.childMethodName,
        methodParameterTypes = command.childMethodParameterTypes,
        methodInput = command.childMethodInput,
        workflowMeta = state.workflowMeta,
        workflowOptions = state.workflowOptions
    )
    sendToWorkflowEngine(msg, 0F)

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
        commandSimpleName = newCommand.commandSimpleName,
        commandStatus = CommandStatusOngoing
    )

    methodRun.pastCommands.add(
        PastCommand(
            commandPosition = newCommand.commandPosition,
            commandType = newCommand.commandType,
            commandId = newCommand.commandId,
            commandHash = newCommand.commandHash,
            commandSimpleName = newCommand.commandSimpleName,
            commandStatus = CommandStatusOngoing
        )
    )

    return pastCommand
}
