package com.zenaton.taskManager.client

import com.zenaton.common.data.SerializedData
import com.zenaton.taskManager.common.data.Task
import com.zenaton.taskManager.common.data.TaskId
import com.zenaton.taskManager.common.data.TaskInput
import com.zenaton.taskManager.common.data.TaskMeta
import com.zenaton.taskManager.common.data.TaskName
import com.zenaton.taskManager.common.data.TaskOptions
import com.zenaton.taskManager.common.data.TaskOutput
import com.zenaton.taskManager.common.exceptions.NoMethodCallAtDispatch
import com.zenaton.taskManager.common.messages.CancelTask
import com.zenaton.taskManager.common.messages.RetryTask
import java.lang.reflect.Proxy

class Client() {
    lateinit var dispatcher: Dispatcher

    fun setAvroDispatcher(avroDispatcher: AvroDispatcher) {
        dispatcher = Dispatcher(avroDispatcher)
    }

    /*
     * Use this method to dispatch a job
     * TODO: using class instance instead of interface is not supported
     */
    inline fun <reified T> dispatchTask(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        method: T.() -> Any?
    ): Task {
        // handler will be where the actual job is done
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
        return handler.getJob() ?: throw NoMethodCallAtDispatch(T::class.java.name)
    }

    /*
     * Use this method to manually retry a job
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
     * Use this method to manually cancel a job
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
