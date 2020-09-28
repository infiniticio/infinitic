package io.infinitic.common.workflows

import io.infinitic.common.tasks.Task

interface WorkflowTaskContext {
    fun <T : Task> proxy(klass: Class<T>): T

    fun <T : Workflow> proxy(klass: Class<T>): T

    fun <T : Task, S> async(proxy: T, method: T.() -> S): Deferred<S>

    fun <T : Workflow, S> async(proxy: T, method: T.() -> S): Deferred<S>

    fun <S> async(branch: () -> S): Deferred<S>

    fun <S> task(inline: () -> S): S

    fun <T> await(deferred: Deferred<T>): Deferred<T>

    fun <T> result(deferred: Deferred<T>): T

    fun <T> status(deferred: Deferred<T>): DeferredStatus
}
