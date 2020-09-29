package io.infinitic.common.workflows

import io.infinitic.common.tasks.Task
import java.lang.reflect.Method

interface WorkflowTaskContext {
    fun <T : Task, S> async(proxy: T, method: T.() -> S): Deferred<S>

    fun <T : Workflow, S> async(proxy: T, method: T.() -> S): Deferred<S>

    fun <S> async(branch: () -> S): Deferred<S>

    fun <S> task(inline: () -> S): S

    fun <T> await(deferred: Deferred<T>): Deferred<T>

    fun <T> result(deferred: Deferred<T>): T

    fun <T> status(deferred: Deferred<T>): DeferredStatus

    fun <T> dispatchTask(method: Method, args: Array<out Any>): Deferred<T>

    fun <T> dispatchWorkflow(method: Method, args: Array<out Any>): Deferred<T>
}
