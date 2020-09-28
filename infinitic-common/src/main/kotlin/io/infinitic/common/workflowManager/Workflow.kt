package io.infinitic.common.workflowManager

import io.infinitic.common.taskManager.Task

interface Workflow {
    val context: WorkflowTaskContext
}

/*
 * Use this to proxy a task
 */
fun <T : Task> Workflow.proxy(klass: Class<out T>) = context.proxy(klass)

/*
 * Use this to proxy a child workflow
 */
fun <T : Workflow> Workflow.proxy(klass: Class<out T>) = context.proxy(klass)

/*
 * Use this to dispatch a proxy task
 */
fun <T : Task, S> Workflow.async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

/*
 * Use this to dispatch a proxy workflow
 */
fun <T : Workflow, S> Workflow.async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

/*
 * Use this to create an async branch
 */
fun <S> Workflow.async(branch: () -> S) = context.async(branch)

/*
 * Use this to create an inline task
 */
fun <S> Workflow.task(inline: () -> S): S = context.task(inline)
