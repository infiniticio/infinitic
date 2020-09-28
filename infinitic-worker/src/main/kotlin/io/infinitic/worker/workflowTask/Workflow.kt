package io.infinitic.worker.workflowTask

import io.infinitic.common.taskManager.Task
import io.infinitic.common.workflowManager.Workflow as WorkflowInterface
import io.infinitic.common.workflowManager.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.worker.workflowTask.deferred.Deferred
import io.infinitic.worker.workflowTask.commands.TaskProxyHandler
import io.infinitic.worker.workflowTask.commands.WorkflowProxyHandler

abstract class Workflow {
    var workflowTaskContext: WorkflowTaskContext? = null
        get() = field ?: throw WorkflowTaskContextNotInitialized(this::class.java.name, WorkflowTaskContext::class.java.name)

    /*
     * Use this method to proxy a task
     */
    protected fun <T : Task> proxy(klass: Class<T>) =
        TaskProxyHandler(klass) { workflowTaskContext!! }.instance()

    /*
     * Use this method to proxy a child workflow
     */
    protected fun <T : WorkflowInterface> proxy(klass: Class<T>) =
        WorkflowProxyHandler(klass) { workflowTaskContext!! }.instance()

    /*
     * Use this method to dispatch a proxy task
     */
    fun <T : Task, S> async(proxy: T, method: T.() -> S): Deferred<S> = workflowTaskContext!!.async(proxy, method)

    /*
     * Use this method to dispatch a proxy workflow
     */
    fun <T : WorkflowInterface, S> async(proxy: T, method: T.() -> S): Deferred<S> = workflowTaskContext!!.async(proxy, method)

    /*
     * Use this method to create an async branch
     */
    fun <S> async(branch: () -> S) = workflowTaskContext!!.async(branch)

    /*
     * Use this method to create an inline task
     */
    fun <S> task(inline: () -> S): S = workflowTaskContext!!.task(inline)
}
