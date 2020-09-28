package io.infinitic.common.workflows

import io.infinitic.common.tasks.Task

interface Workflow {
    val context: WorkflowTaskContext
}

/*
 * Proxy a task
 */
fun <T : Task> Workflow.proxy(klass: Class<out T>) = context.proxy(klass)

/*
 * Proxy a child workflow
 */
fun <T : Workflow> Workflow.proxy(klass: Class<out T>) = context.proxy(klass)

/*
 * Dispatch a task
 */
fun <T : Task, S> Workflow.async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

/*
 * Dispatch a workflow
 */
fun <T : Workflow, S> Workflow.async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

/*
 * Create an async branch
 */
fun <S> Workflow.async(branch: () -> S) = context.async(branch)

/*
 * Create an inline task
 */
fun <S> Workflow.task(inline: () -> S): S = context.task(inline)
