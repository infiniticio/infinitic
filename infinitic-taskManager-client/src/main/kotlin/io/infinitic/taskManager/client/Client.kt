package io.infinitic.taskManager.client

import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Task
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.taskManager.common.exceptions.NoMethodCallAtDispatch
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.RetryTask
import java.lang.reflect.Proxy

class Client() {
    lateinit var dispatcher: Dispatcher

    fun setAvroDispatcher(avroDispatcher: AvroDispatcher) {
        dispatcher = Dispatcher(avroDispatcher)
    }

    /*
     * Use this method to dispatch a task
     * TODO: using class instance instead of interface is not supported
     */
    inline fun <reified T> dispatchTask(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        method: T.() -> Any?
    ): Task {
        // handler will be where the actual task is done
        val handler = ProxyHandler(T::class.java.name, dispatcher, options, meta)

        // get a proxy for T
        val klass = Proxy.newProxyInstance(
            T::class.java.classLoader,
            kotlin.arrayOf(T::class.java),
            handler
        ) as T

        // method call will actually be applied to handler through the proxy
        klass.method()

        // ask
        return handler.getTask() ?: throw NoMethodCallAtDispatch(T::class.java.name)
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
        dispatcher.toTaskEngine(msg)
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
            taskOutput = TaskOutput(SerializedData.from(output))
        )
        dispatcher.toTaskEngine(msg)
    }
}
