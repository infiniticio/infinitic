package io.infinitic.client.taskManager

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskInstance
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskName
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.common.taskManager.data.TaskOutput
import io.infinitic.common.taskManager.exceptions.NoMethodCallAtDispatch
import io.infinitic.common.taskManager.messages.CancelTask
import io.infinitic.common.taskManager.messages.DispatchTask
import io.infinitic.common.taskManager.messages.RetryTask
import io.infinitic.common.taskManager.proxies.MethodProxyHandler

open class Client(open val dispatcher: Dispatcher) {

    /*
     * Use this method to dispatch a task
     */
    suspend inline fun <reified T> dispatchTask(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        apply: T.() -> Any?
    ): TaskInstance {
        // TODO: using class instead of interface is not supported (CGLIB or JavaAssist could be used)
        // get a proxy for T
        val handler = MethodProxyHandler()

        // get a proxy instance
        val klass = handler.instance<T>()

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
        dispatcher.toTaskEngine(msg)

        return TaskInstance(msg.taskId)
    }

    /*
     * Use this method to manually retry a task
     * when a non-null parameter is provided, it will supersede current one
     */
    suspend fun retryTask(
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
        dispatcher.toTaskEngine(msg)
    }

    /*
     * Use this method to manually cancel a task
     */
    suspend fun cancelTask(
        id: String,
        output: Any? = null
    ) {
        val msg = CancelTask(
            taskId = TaskId(id),
            taskOutput = TaskOutput(output)
        )
        dispatcher.toTaskEngine(msg)
    }
}
