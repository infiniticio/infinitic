package io.infinitic.taskManager.client

import io.infinitic.taskManager.common.data.Task
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.exceptions.NoMethodCallAtDispatch
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.taskManager.common.messages.RetryTask
import java.lang.reflect.Proxy

open class Client() {
    lateinit var taskDispatcher: TaskDispatcher

    open fun setDispatcher(avroDispatcher: AvroTaskDispatcher) {
        taskDispatcher = TaskDispatcher(avroDispatcher)
    }

    /*
     * Use this method to dispatch a task
     * TODO: using class instance instead of interface is not supported
     */
    inline fun <reified T> dispatchTask(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        apply: T.() -> Any?
    ): Task {
        // get a proxy for T
        val handler = ProxyHandler()

        val klass = Proxy.newProxyInstance(
            T::class.java.classLoader,
            kotlin.arrayOf(T::class.java),
            handler
        ) as T

        // method call will actually be done through the proxy by handler
        klass.apply()

        // dispatch the workflow
        val method = handler.method ?: throw NoMethodCallAtDispatch(T::class.java.name, "dispatchTask")

        val msg = DispatchTask(
            taskId = TaskId(),
            taskName = TaskName.from(method),
            taskInput = TaskInput.from(method, handler.args),
            taskOptions = options,
            taskMeta = meta.withParametersTypesFrom(method)
        )
        taskDispatcher.toTaskEngine(msg)

        return Task(msg.taskId)
    }

    /*
     * Use this method to manually retry a task
     * when a non-null parameter is provided, it will supersede current one
     */
    fun retryTask(
        id: String,
        name: TaskName? = null,
        input: TaskInput? = null,
        options: TaskOptions? = null,
        meta: TaskMeta? = null
    ) {
        val msg = RetryTask(
            taskId = TaskId(id),
            taskName = name,
            taskInput = input,
            taskOptions = options,
            taskMeta = meta
        )
        taskDispatcher.toTaskEngine(msg)
    }

    /*
     * Use this method to manually cancel a task
     */
    fun cancelTask(
        id: String,
        output: Any? = null
    ) {
        val msg = CancelTask(
            taskId = TaskId(id),
            taskOutput = TaskOutput(output)
        )
        taskDispatcher.toTaskEngine(msg)
    }
}
