package com.zenaton.sdk

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobMeta
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.messages.DispatchJob
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyTaskHandler : InvocationHandler {
    lateinit var dispatcher: Dispatcher

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {

        val parameterTypes = method.parameterTypes.map { it.name }

        parameterTypes.map { println(convertPrimitive(it)) }

        val input = args?.mapIndexed { index: Int, param: Any? -> if (param == null) parameterTypes[index] else param }
        input?.map { println(it) }

        return "toto"
    }

    private fun convertPrimitive(klass: String) = when (klass) {
        "bytes" -> Byte::class.java
        "short" -> Short::class.java
        "int" -> Int::class.java
        "long" -> Long::class.java
        "float" -> Float::class.java
        "double" -> Double::class.java
        "boolean" -> Boolean::class.java
        "char" -> Character::class.java
        else -> klass
    }

    private fun dispatchJob(name: String, method: String, parameterTypes: List<String>, args: List<Any?>) {
        val m: Method
        val jobAttemptStarted = DispatchJob(
            jobId = JobId(),
            jobName = JobName("$name::$method"),
            jobInput = JobInput(args.map { SerializedData.from(it) }),
            jobMeta = JobMeta.builder().add("javaParameterTypes", parameterTypes).build()
        )

        dispatcher.toJobEngine(jobAttemptStarted)
    }
}
