package com.zenaton.pulsar.utils

import com.zenaton.engine.topics.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.topics.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.topics.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.topics.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.engine.topics.tasks.state.TaskState
import com.zenaton.messages.taskAttempts.AvroTaskAttemptDispatched
import com.zenaton.messages.tasks.AvroTaskAttemptCompleted
import com.zenaton.messages.tasks.AvroTaskAttemptFailed
import com.zenaton.messages.tasks.AvroTaskAttemptRetried
import com.zenaton.messages.tasks.AvroTaskAttemptStarted
import com.zenaton.messages.tasks.AvroTaskAttemptTimeout
import com.zenaton.messages.tasks.AvroTaskDispatched
import com.zenaton.states.AvroTaskState
import com.zenaton.utils.json.Json
import kotlin.reflect.KClass

/**
 * This class does the mapping between avro-generated classes and classes used by our code
 */
object AvroConverter {
    /**
     *  Task State
     */
    fun toAvro(obj: TaskState) = convert(obj, AvroTaskState::class)
    fun fromAvro(obj: AvroTaskState) = convert(obj, TaskState::class)

    /**
     *  Task Attempts Messages
     */
    fun toAvro(obj: TaskAttemptDispatched) = convert(obj, AvroTaskAttemptDispatched::class)
    fun fromAvro(obj: AvroTaskAttemptDispatched) = convert(obj, TaskAttemptDispatched::class)

    /**
     *  Tasks Messages
     */
    fun toAvro(obj: TaskAttemptCompleted) = convert(obj, AvroTaskAttemptCompleted::class)
    fun fromAvro(obj: AvroTaskAttemptCompleted) = convert(obj, TaskAttemptCompleted::class)

    fun toAvro(obj: TaskAttemptFailed) = convert(obj, AvroTaskAttemptFailed::class)
    fun fromAvro(obj: AvroTaskAttemptFailed) = convert(obj, TaskAttemptFailed::class)

    fun toAvro(obj: TaskAttemptRetried) = convert(obj, AvroTaskAttemptRetried::class)
    fun fromAvro(obj: AvroTaskAttemptRetried) = convert(obj, TaskAttemptRetried::class)

    fun toAvro(obj: TaskAttemptStarted) = convert(obj, AvroTaskAttemptStarted::class)
    fun fromAvro(obj: AvroTaskAttemptStarted) = convert(obj, TaskAttemptStarted::class)

    fun toAvro(obj: TaskAttemptTimeout) = convert(obj, AvroTaskAttemptTimeout::class)
    fun fromAvro(obj: AvroTaskAttemptTimeout) = convert(obj, TaskAttemptTimeout::class)

    fun toAvro(obj: TaskDispatched) = convert(obj, AvroTaskDispatched::class)
    fun fromAvro(obj: AvroTaskDispatched) = convert(obj, TaskDispatched::class)

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private fun <T : Any> convert(from: Any, to: KClass<T>): T = Json.parse(Json.stringify(from), to)
}
