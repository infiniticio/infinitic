package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.exceptions.NoMethodCallAtDispatch
import io.infinitic.taskManager.common.proxies.MethodProxyHandler
import io.infinitic.workflowManager.common.data.commands.CommandSimpleName
import io.infinitic.workflowManager.common.data.commands.DispatchTask
import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.data.MethodContext
import java.lang.reflect.Method

abstract class Workflow {
    private lateinit var methodContext: MethodContext

    fun setMethodContext(methodContext: MethodContext) {
        this.methodContext = methodContext
    }

    fun getBranchContext(): MethodContext {
        if (! this::methodContext.isInitialized) throw WorkflowTaskContextNotInitialized(this::class.java.name, MethodContext::class.java.name)

        return methodContext
    }

    /*
    * Use this method to proxy task or child workflows
    */
    protected inline fun <reified T : Any> proxy() = TaskProxyHandler(this).instance<T>()

    /*
     * Use this method to dispatch a task
    */
    inline fun <reified T : Any, reified S> async(
        proxy: T,
        apply: T.() -> S
    ): Deferred<S> {
        // get a proxy for T
        val handler = MethodProxyHandler()

        // get a proxy instance
        val klass = handler.instance<T>()

        // this call will capture method and arguments
        klass.apply()

        // retrieve method
        val method = handler.method ?: throw NoMethodCallAtDispatch(T::class.java.name, "async")

        // dispatch this request
        return dispatch(method, handler.args, S::class.java)
    }

    public fun <S> dispatch(method: Method, args: Array<out Any>, type: Class<S>): Deferred<S> {
        // increment current command index
        methodContext.next()
        // set current command
        val dispatch = DispatchTask(
            taskName = TaskName.from(method),
            taskInput = TaskInput(*args)
        )
        val newCommand = NewCommand(
            command = dispatch,
            commandSimpleName = CommandSimpleName.fromMethod(method),
            commandHash = dispatch.hash(),
            commandPosition = methodContext.currentMethodPosition.position
        )

        val pastCommand = methodContext.getPastInstructionSimilarTo(newCommand) as PastCommand?

        when (pastCommand) {
            // this is a new command, we add it to the newCommands list
            null -> methodContext.newCommands.add(newCommand)
            // this command is already known, do nothing
            else -> Unit
        }

        return Deferred<S>(
            id = newCommand.commandId.id,
            newCommand = newCommand,
            pastCommand = pastCommand
        )
    }
}
