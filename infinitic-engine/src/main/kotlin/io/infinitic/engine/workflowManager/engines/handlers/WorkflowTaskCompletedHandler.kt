// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engine.workflowManager.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.workflows.data.commands.CommandOutput
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflows.data.commands.DispatchReceiver
import io.infinitic.common.workflows.data.commands.DispatchTask as DispatchTaskInWorkflow
import io.infinitic.common.workflows.data.commands.DispatchTimer
import io.infinitic.common.workflows.data.commands.EndAsync
import io.infinitic.common.workflows.data.commands.EndInlineTask
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.StartAsync
import io.infinitic.common.workflows.data.commands.StartInlineTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.steps.StepStatusOngoing
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex
import io.infinitic.common.workflows.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.messages.WorkflowCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.infinitic.common.workflows.data.states.WorkflowState
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.messages.DispatchWorkflow
import io.infinitic.engine.workflowManager.engines.WorkflowEngine

class WorkflowTaskCompletedHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(state: WorkflowState, msg: WorkflowTaskCompleted) {
        // remove currentWorkflowTaskId
        state.currentWorkflowTaskId = null

        val workflowTaskOutput = msg.workflowTaskOutput
        val methodRun = getMethodRun(state, workflowTaskOutput.methodRunId)
        // add new commands to past instructions
        workflowTaskOutput.newCommands.map {
            when (val command = it.command) {
                is DispatchTaskInWorkflow -> dispatchTask(methodRun, it, msg.workflowId)
                is DispatchChildWorkflow -> dispatchChildWorkflow(methodRun, it, state)
                is StartAsync -> startAsync(methodRun, it)
                is EndAsync -> endAsync(methodRun, it, state.currentMessageIndex)
                is StartInlineTask -> startInlineTask(methodRun, it)
                is EndInlineTask -> endInlineTask(methodRun, it, state.currentMessageIndex)
                is DispatchTimer -> TODO()
                is DispatchReceiver -> TODO()
            }
        }
        // add new steps to past instructions
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

        // update current workflow properties
        workflowTaskOutput.workflowPropertiesUpdates.map {
            TODO()
        }

        // if completed for the first time
        if (workflowTaskOutput.methodOutput != null && methodRun.methodOutput == null) {
            // set methodOutput in state
            methodRun.methodOutput = workflowTaskOutput.methodOutput

            // if this is the main method, it means the workflow is completed
            if (methodRun.isMain) {
                dispatcher.toWorkflowEngine(
                    WorkflowCompleted(
                        workflowId = state.workflowId,
                        workflowOutput = methodRun.methodOutput!!
                    )
                )
            }

            // tell parent workflow if any
            methodRun.parentWorkflowId?.let {
                dispatcher.toWorkflowEngine(
                    ChildWorkflowCompleted(
                        workflowId = it,
                        methodRunId = methodRun.parentMethodRunId!!,
                        childWorkflowId = state.workflowId,
                        childWorkflowOutput = workflowTaskOutput.methodOutput!!
                    )
                )
            }
        }

        // if everything is completed in methodRun then filter state
        cleanMethodRun(methodRun, state)
    }

    private fun startAsync(methodRun: MethodRun, newCommand: NewCommand) {
        addPastCommand(methodRun, newCommand)
    }

    private fun endAsync(methodRun: MethodRun, newCommand: NewCommand, currentMessageIndex: WorkflowMessageIndex) {
        val command = newCommand.command as EndAsync
        // look for previous Start Async command
        val pastStartAsync = methodRun.pastCommands.first {
            it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_ASYNC
        }
        // past command completed
        pastStartAsync.commandStatus = CommandStatusCompleted(
            CommandOutput(command.asyncOutput.data),
            currentMessageIndex
        )
    }

    private fun startInlineTask(methodRun: MethodRun, newCommand: NewCommand) {
        addPastCommand(methodRun, newCommand)
    }

    private fun endInlineTask(methodRun: MethodRun, newCommand: NewCommand, currentMessageIndex: WorkflowMessageIndex) {
        val command = newCommand.command as EndInlineTask
        // look for previous StartInlineTask command
        val pastStartInlineTask = methodRun.pastCommands.first {
            it.commandPosition == newCommand.commandPosition && it.commandType == CommandType.START_INLINE_TASK
        }
        // past command completed
        pastStartInlineTask.commandStatus = CommandStatusCompleted(
            CommandOutput(command.inlineTaskOutput.data),
            currentMessageIndex
        )
    }

    private suspend fun dispatchTask(methodRun: MethodRun, newCommand: NewCommand, workflowId: WorkflowId) {
        val command = newCommand.command as DispatchTaskInWorkflow
        // send task to task engine
        val msg = DispatchTask(
            taskId = TaskId("${newCommand.commandId}"),
            taskName = command.taskName,
            taskMethod = command.taskMethod,
            taskInput = command.taskInput,
            taskMeta = command.taskMeta
                .with<TaskMeta>(WorkflowEngine.META_WORKFLOW_ID, workflowId)
                .with<TaskMeta>(WorkflowEngine.META_METHOD_RUN_ID, methodRun.methodRunId)
        )
        dispatcher.toTaskEngine(msg)

        addPastCommand(methodRun, newCommand)
    }

    private suspend fun dispatchChildWorkflow(methodRun: MethodRun, newCommand: NewCommand, state: WorkflowState) {
        val command = newCommand.command as DispatchChildWorkflow
        // send task to task engine
        val msg = DispatchWorkflow(
            workflowId = WorkflowId("${newCommand.commandId}"),
            parentWorkflowId = state.workflowId,
            parentMethodRunId = methodRun.methodRunId,
            workflowName = command.childWorkflowName,
            methodName = command.childMethodName,
            methodInput = command.childMethodInput,
            workflowMeta = state.workflowMeta,
            workflowOptions = state.workflowOptions
        )
        dispatcher.toWorkflowEngine(msg)

        addPastCommand(methodRun, newCommand)
    }

    private fun addPastCommand(methodRun: MethodRun, newCommand: NewCommand) {
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
    }
}
