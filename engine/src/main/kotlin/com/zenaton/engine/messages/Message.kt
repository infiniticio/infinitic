package com.zenaton.engine.messages

import com.zenaton.engine.common.serializer.JavaSerDeSerializer

class Message(val type: MessageType?, val msg: ByteArray) {
    // empty constructor needed during serialization
    constructor() : this(null, ByteArray(0))

    constructor(msg: WorkflowDispatched) : this(MessageType.WORKFLOW_DISPATCHED, JavaSerDeSerializer.serialize(msg))
    constructor(msg: TaskDispatched) : this(MessageType.TASK_DISPATCHED, JavaSerDeSerializer.serialize(msg))

    // Returns typed message, after having checked type consistency
    inline fun <reified T> get(): T = when (type?.className) {
        T::class.qualifiedName -> JavaSerDeSerializer.deserialize<T>(msg)
        else -> throw UnsupportedOperationException()
    }
}
