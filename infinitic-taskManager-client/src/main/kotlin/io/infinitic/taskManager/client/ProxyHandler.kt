package io.infinitic.taskManager.client

import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.data.SerializedData
import io.infinitic.taskManager.common.data.Task
import io.infinitic.taskManager.common.Constants
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.exceptions.ErrorDuringJsonDeserializationOfParameter
import io.infinitic.taskManager.common.exceptions.ErrorDuringJsonSerializationOfParameter
import io.infinitic.taskManager.common.exceptions.InconsistentJsonSerializationOfParameter
import io.infinitic.taskManager.common.exceptions.MultipleMethodCallsAtDispatch
import io.infinitic.taskManager.common.messages.DispatchTask
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyHandler(
    private val className: String,
    private val dispatcher: Dispatcher,
    private val taskOptions: TaskOptions,
    private val taskMeta: TaskMeta
) : InvocationHandler {
    private var taskId: TaskId? = null

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (taskId != null) throw MultipleMethodCallsAtDispatch(className)

        taskId = TaskId()
        val msg = DispatchTask(
            taskId = taskId!!,
            taskName = TaskName("$className${Constants.METHOD_DIVIDER}${method.name}"),
            taskInput = TaskInput(args?.mapIndexed { index, value -> getSerializedData(method.parameters[index].name, value, method.parameterTypes[index], method.name, className) } ?: listOf()),
            taskOptions = taskOptions,
            taskMeta = taskMeta.setParameterTypes(method.parameterTypes.map { it.name })
        )
        dispatcher.toTaskEngine(msg)

        return null
    }

    private fun getSerializedData(parameterName: String, parameterValue: Any?, parameterType: Class<*>, methodName: String, className: String): SerializedData {
        val data: SerializedData
        val restoredValue: Any?
        // serialize data
        try {
            data = SerializedData.from(parameterValue)
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonSerializationOfParameter(parameterName, parameterValue, parameterType.name, methodName, className)
        }
        // for user convenience, we check here that data can actually be deserialized
        try {
            restoredValue = data.deserialize()
        } catch (e: JsonProcessingException) {
            throw ErrorDuringJsonDeserializationOfParameter(parameterName, parameterValue, parameterType.name, methodName, className)
        }
        // check that serialization/deserialization process works as expected
        if (parameterValue != restoredValue) throw InconsistentJsonSerializationOfParameter(parameterName, parameterValue, restoredValue, parameterType.name, methodName, className)

        return data
    }

    fun getTask() = taskId?.let { Task(it) }
}
