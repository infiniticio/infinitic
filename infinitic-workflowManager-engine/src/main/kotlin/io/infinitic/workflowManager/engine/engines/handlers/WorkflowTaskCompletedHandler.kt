package io.infinitic.workflowManager.engine.engines.handlers

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.workflowManager.common.data.commands.CommandStatusOngoing
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.common.data.commands.CommandStatusCompleted
import io.infinitic.workflowManager.common.data.commands.CommandType
import io.infinitic.workflowManager.common.data.commands.DispatchChildWorkflow
import io.infinitic.workflowManager.common.data.commands.DispatchReceiver
import io.infinitic.workflowManager.common.data.commands.DispatchTask as DispatchTaskInWorkflow
import io.infinitic.workflowManager.common.data.commands.DispatchTimer
import io.infinitic.workflowManager.common.data.commands.EndAsync
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.commands.StartAsync
import io.infinitic.workflowManager.common.data.instructions.PastStep
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.steps.StepStatusOngoing
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
import io.infinitic.workflowManager.common.messages.ChildWorkflowCompleted
import io.infinitic.workflowManager.common.messages.WorkflowCompleted
import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
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
                is DispatchChildWorkflow -> TODO()
                is StartAsync -> startAsync(methodRun, it)
                is EndAsync -> endAsync(methodRun, it, state.currentMessageIndex)
                is DispatchTimer -> TODO()
                is DispatchReceiver -> TODO()
            }
        }
        // add new steps to past instructions
        workflowTaskOutput.newSteps.map {
            methodRun.pastInstructions.add(
                PastStep(
                    stringPosition = it.stepStringPosition,
                    step = it.step,
                    stepHash = it.stepHash,
                    stepStatus = StepStatusOngoing()
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
                        childWorkflowId = state.workflowId,
                        childOutput = workflowTaskOutput.methodOutput
                    )
                )
            }
        }

        // if everything is completed in methodRun then filter state
        if (methodRun.methodOutput != null && methodRun.pastInstructions.all { it.isTerminated() }) {
            // TODO("filter workflow if unused properties")
            state.currentMethodRuns.remove(methodRun)
        }
    }

    private fun startAsync(methodRun: MethodRun, newCommand: NewCommand) {
        addPastCommand(methodRun, newCommand)
    }

    private fun endAsync(methodRun: MethodRun, newCommand: NewCommand, currentMessageIndex: WorkflowMessageIndex) {
        val command = newCommand.command as EndAsync
        // look for previous Start Async command
        val pastStartAsync = methodRun.pastInstructions.first {
            it.stringPosition == newCommand.commandStringPosition &&
                it is PastCommand &&
                it.commandType == CommandType.START_ASYNC
        } as PastCommand
        // past command completed
        pastStartAsync.commandStatus = CommandStatusCompleted(command.asyncOutput.data, currentMessageIndex)
    }

    private suspend fun dispatchTask(methodRun: MethodRun, newCommand: NewCommand, workflowId: WorkflowId) {
        val command = newCommand.command as DispatchTaskInWorkflow
        // send task to task engine
        val task = DispatchTask(
            taskId = TaskId("${newCommand.commandId}"),
            taskName = command.taskName,
            taskInput = command.taskInput,
            taskMeta = command.taskMeta
                .with<TaskMeta>(WorkflowEngine.META_WORKFLOW_ID, workflowId)
                .with<TaskMeta>(WorkflowEngine.META_METHOD_RUN_ID, methodRun.methodRunId)
        )
        dispatcher.toTaskEngine(task)

        addPastCommand(methodRun, newCommand)
    }

    private fun addPastCommand(methodRun: MethodRun, newCommand: NewCommand) {
        methodRun.pastInstructions.add(
            PastCommand(
                stringPosition = newCommand.commandStringPosition,
                commandType = newCommand.commandType,
                commandId = newCommand.commandId,
                commandHash = newCommand.commandHash,
                commandSimpleName = newCommand.commandSimpleName,
                commandStatus = CommandStatusOngoing()
            )
        )
    }
}
