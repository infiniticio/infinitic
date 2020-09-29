package io.infinitic.worker.workflowTask

import io.infinitic.common.tasks.Task
import io.infinitic.common.tasks.proxies.MethodProxyHandler
import io.infinitic.common.workflows.Deferred
import io.infinitic.common.workflows.DeferredStatus
import io.infinitic.common.workflows.data.commands.Command
import io.infinitic.common.workflows.data.commands.CommandOutput
import io.infinitic.common.workflows.data.commands.CommandSimpleName
import io.infinitic.common.workflows.data.commands.CommandStatusCanceled
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.commands.DispatchChildWorkflow
import io.infinitic.common.workflows.data.commands.DispatchTask
import io.infinitic.common.workflows.data.commands.EndAsync
import io.infinitic.common.workflows.data.commands.EndInlineTask
import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.commands.StartAsync
import io.infinitic.common.workflows.data.commands.StartInlineTask
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.steps.PastStep
import io.infinitic.common.workflows.data.steps.Step
import io.infinitic.common.workflows.data.steps.StepStatusCanceled
import io.infinitic.common.workflows.data.steps.StepStatusCompleted
import io.infinitic.common.workflows.data.steps.StepStatusOngoing
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.exceptions.NoMethodCallAtAsync
import io.infinitic.common.workflows.exceptions.ShouldNotWaitInInlineTask
import io.infinitic.common.workflows.exceptions.WorkflowUpdatedWhileRunning
import io.infinitic.common.workflows.parser.setPropertiesToObject
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.WorkflowTaskContext
import java.lang.reflect.Method

class WorkflowTaskContextImpl(
    private val workflowTaskInput: WorkflowTaskInput,
    private val workflowInstance: Workflow
) : WorkflowTaskContext {
    // current position in the tree of method processing
    private var methodLevel: MethodLevel = MethodLevel(messageIndex = workflowTaskInput.methodRun.messageIndexAtStart)

    // new commands (if any) discovered during execution of the method
    var newCommands: MutableList<NewCommand> = mutableListOf()

    // new steps (if any) discovered during execution the method
    var newSteps: MutableList<NewStep> = mutableListOf()

    /*
     * Async Task dispatching:
     */
    override fun <T : Task, S> async(
        proxy: T,
        method: T.() -> S
    ): Deferred<S> {
        val command = Class.forName(proxy.toString())

        // get a proxy for T
        val handler = MethodProxyHandler(command)

        // get a proxy instance
        @Suppress("UNCHECKED_CAST")
        val klass = handler.instance() as T

        // this call will capture method and arguments
        klass.method()

        return dispatchTask<S>(
            handler.method ?: throw NoMethodCallAtAsync(command::class.java.name),
            handler.args
        )
    }

    /*
     * Async Task dispatching:
     */
    override fun <T : Workflow, S> async(
        proxy: T,
        method: T.() -> S
    ): Deferred<S> {
        val command = Class.forName(proxy.toString())

        // get a proxy for T
        val handler = MethodProxyHandler(command)

        // get a proxy instance
        @Suppress("UNCHECKED_CAST")
        val klass = handler.instance() as T

        // this call will capture method and arguments
        klass.method()

        return dispatchWorkflow<S>(
            handler.method ?: throw NoMethodCallAtAsync(command::class.java.name),
            handler.args
        )
    }

    /*
     * Async Branch dispatching:
     */
    override fun <S> async(branch: () -> S): Deferred<S> {
        // increment position
        positionNext()

        // set a new command
        val dispatch = StartAsync
        // create instruction that *may* be sent to engine
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName("${CommandType.START_ASYNC}"),
            commandPosition = methodLevel.methodPosition
        )

        val pastCommand = getPastCommandSimilarTo(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)
            // run async branch
            runAsync(branch)

            // returns a Deferred with an ongoing step
            return Deferred<S>(Step.Id.from(newCommand), this)
        } else {
            // branch is processed only if not yet completed or canceled
            if (pastCommand.commandStatus is CommandStatusOngoing) {
                runAsync(branch)
            }

            // returns a Deferred linked to pastCommand
            return Deferred<S>(Step.Id.from(pastCommand), this)
        }
    }

    /*
     * Inline task
     */
    override fun <S> task(inline: () -> S): S {
        // increment position
        positionNext()

        // set a new command
        val dispatch = StartInlineTask
        // create instruction that *may* be sent to engine
        val startCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName("${CommandType.START_INLINE_TASK}"),
            commandPosition = methodLevel.methodPosition
        )

        val pastCommand = getPastCommandSimilarTo(startCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(startCommand)
            // go down (it should be needed only if inline task dispatch some tasks)
            positionDown()
            // run inline task
            val commandOutput = try {
                CommandOutput(inline())
            } catch (e: Exception) {
                when (e) {
                    is NewStepException -> throw ShouldNotWaitInInlineTask()
                    is KnownStepException -> throw ShouldNotWaitInInlineTask()
                    else -> throw e
                }
            }
            // go up
            positionUp()
            // record result
            val endCommand = NewCommand(
                command = EndInlineTask(commandOutput),
                commandSimpleName = CommandSimpleName("${CommandType.END_INLINE_TASK}"),
                commandPosition = methodLevel.methodPosition
            )
            newCommands.add(endCommand)
            // returns a Deferred with an ongoing step
            @Suppress("UNCHECKED_CAST")
            return commandOutput.data as S
        } else {
            @Suppress("UNCHECKED_CAST")
            return when (val status = pastCommand.commandStatus) {
                is CommandStatusOngoing -> throw RuntimeException("This should not happen: uncompleted inline task")
                is CommandStatusCanceled -> throw RuntimeException("This should not happen: canceled inline task")
                is CommandStatusCompleted -> status.completionResult.data as S
            }
        }
    }

    /*
     * Deferred await()
     */
    override fun <T> await(deferred: Deferred<T>): Deferred<T> {
        // increment position
        positionNext()
        // create a new step
        val newStep = NewStep(
            step = deferred.step,
            stepPosition = methodLevel.methodPosition
        )
        val pastStep = getPastStepSimilarTo(newStep)

        // if this is really a new step, we check its status based on current workflow message index
        if (pastStep == null) {
            deferred.stepStatus = newStep.step.stepStatusAtMessageIndex(methodLevel.messageIndex)
            // if this deferred is still ongoing,
            if (deferred.stepStatus is StepStatusOngoing) {
                // we add a new step
                newSteps.add(newStep)
                // and stop here
                throw NewStepException()
            }
            // if this deferred is already terminated, we continue
            return deferred
        }

        // set status
        deferred.stepStatus = pastStep.stepStatus

        // throw KnownStepException if ongoing else else update message index
        methodLevel.messageIndex = when (val stepStatus = deferred.stepStatus) {
            is StepStatusOngoing -> throw KnownStepException()
            is StepStatusCompleted -> stepStatus.completionWorkflowMessageIndex
            is StepStatusCanceled -> stepStatus.cancellationWorkflowMessageIndex
        }

        // update workflow instance properties
        val properties = pastStep.propertiesAtTermination!!.mapValues { workflowTaskInput.workflowPropertyStore[it.value] }
        setPropertiesToObject(workflowInstance, properties)

        // continue
        return deferred
    }

    /*
     * Deferred result()
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> result(deferred: Deferred<T>): T = when (val status = await(deferred).stepStatus) {
        is StepStatusOngoing -> throw RuntimeException("THIS SHOULD NOT HAPPEN: asking result of an ongoing deferred")
        is StepStatusCompleted -> status.completionResult.data as T
        is StepStatusCanceled -> status.cancellationResult.data as T
    }

    /*
     * Deferred status()
     */
    override fun <T> status(deferred: Deferred<T>): DeferredStatus = when (deferred.step.stepStatusAtMessageIndex(methodLevel.messageIndex)) {
        is StepStatusOngoing -> DeferredStatus.ONGOING
        is StepStatusCompleted -> DeferredStatus.COMPLETED
        is StepStatusCanceled -> DeferredStatus.CANCELED
    }

    /*
     * Task dispatching
     */
    override fun <S> dispatchTask(method: Method, args: Array<out Any>) =
        dispatch<S>(DispatchTask.from(method, args), CommandSimpleName.fromMethod(method))

    /*
     * Workflow dispatching
     */
    override fun <S> dispatchWorkflow(method: Method, args: Array<out Any>) =
        dispatch<S>(DispatchChildWorkflow.from(method, args), CommandSimpleName.fromMethod(method))

    /*
     * Go to next position within the same branch
     */
    private fun positionNext() {
        methodLevel = methodLevel.next()
    }

    /*
     * Go to parent branch, this is done at the end of a async { ... } function
     */
    private fun positionUp() {
        methodLevel.up()?.let { methodLevel = it }
    }

    /*
     * Go to child branch, this is done at the start of a async { ... } function
     */
    private fun positionDown() {
        methodLevel = methodLevel.down()
    }

    private fun <S> dispatch(command: Command, commandSimpleName: CommandSimpleName): Deferred<S> {
        // increment position
        positionNext()

        // create instruction that may be sent to engine
        val newCommand = NewCommand(
            command = command,
            commandSimpleName = commandSimpleName,
            commandPosition = methodLevel.methodPosition
        )

        val pastCommand = getPastCommandSimilarTo(newCommand)

        if (pastCommand == null) {
            // if this is a new command, we add it to the newCommands list
            newCommands.add(newCommand)
            // and returns a Deferred with an ongoing step
            return Deferred<S>(Step.Id.from(newCommand), this)
        } else {
            // else ew return a Deferred linked to pastCommand
            return Deferred<S>(Step.Id.from(pastCommand), this)
        }
    }

    /*
     * When running an async branch:
     * - unknown step breaks only current branch
     * - output is returned through a special EndAsync command
     */
    private fun <S> runAsync(branch: () -> S) {
        // go down
        positionDown()

        val commandOutput = try {
            CommandOutput(branch())
        } catch (e: Exception) {
            when (e) {
                is NewStepException -> null
                is KnownStepException -> null
                else -> throw e
            }
        }
        // go up - note that message index comes back to its previous value before going down
        positionUp()

        // if the branch is completed, then we send a special command to the engine
        if (commandOutput != null) {
            // create instruction that *may* be sent to engine
            val newCommand = NewCommand(
                command = EndAsync(commandOutput),
                commandSimpleName = CommandSimpleName("${CommandType.END_ASYNC}"),
                commandPosition = methodLevel.methodPosition
            )
            newCommands.add(newCommand)
        }
    }

    private fun getPastCommandSimilarTo(newCommand: NewCommand): PastCommand? {
        // find pastCommand in current position
        val pastCommand = workflowTaskInput.methodRun.pastCommands
            .find { it.commandPosition == methodLevel.methodPosition }

        // if it exists, check it has not changed
        if (pastCommand != null && !pastCommand.isSimilarTo(newCommand, workflowTaskInput.workflowOptions.workflowChangeCheckMode)) {
            throw WorkflowUpdatedWhileRunning(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${methodLevel.methodPosition}"
            )
        }

        return pastCommand
    }

    private fun getPastStepSimilarTo(newStep: NewStep): PastStep? {
        // find pastCommand in current position
        val pastStep = workflowTaskInput.methodRun.pastSteps
            .find { it.stepPosition == methodLevel.methodPosition }

        // if it exists, check it has not changed
        if (pastStep != null && !pastStep.isSimilarTo(newStep)) {
            throw WorkflowUpdatedWhileRunning(
                workflowTaskInput.workflowName.name,
                "${workflowTaskInput.methodRun.methodName}",
                "${methodLevel.methodPosition}"
            )
        }

        return pastStep
    }
}
