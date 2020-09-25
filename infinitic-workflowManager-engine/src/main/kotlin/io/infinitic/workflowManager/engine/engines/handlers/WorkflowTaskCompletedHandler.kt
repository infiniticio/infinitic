package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.workflowManager.data.commands.CommandStatusOngoing
import io.infinitic.common.taskManager.messages.DispatchTask
import io.infinitic.common.workflowManager.data.commands.CommandOutput
import io.infinitic.common.workflowManager.data.commands.CommandStatusCompleted
import io.infinitic.common.workflowManager.data.commands.CommandType
import io.infinitic.common.workflowManager.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflowManager.data.commands.DispatchReceiver
import io.infinitic.common.workflowManager.data.commands.DispatchTask as DispatchTaskInWorkflow
import io.infinitic.common.workflowManager.data.commands.DispatchTimer
import io.infinitic.common.workflowManager.data.commands.EndAsync
import io.infinitic.common.workflowManager.data.commands.EndInlineTask
import io.infinitic.common.workflowManager.data.commands.NewCommand
import io.infinitic.common.workflowManager.data.commands.PastCommand
import io.infinitic.common.workflowManager.data.commands.StartAsync
import io.infinitic.common.workflowManager.data.commands.StartInlineTask
import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.steps.StepStatusOngoing
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.workflows.WorkflowMessageIndex
import io.infinitic.common.workflowManager.messages.ChildWorkflowCompleted
import io.infinitic.common.workflowManager.messages.WorkflowCompleted
import io.infinitic.common.workflowManager.messages.WorkflowTaskCompleted
import io.infinitic.common.workflowManager.data.states.WorkflowState
import io.infinitic.common.workflowManager.data.steps.PastStep
import io.infinitic.common.workflowManager.messages.DispatchWorkflow
import io.infinitic.workflowManager.engine.engines.WorkflowEngine

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
