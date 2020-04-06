package com.zenaton.engine.messages

import com.zenaton.engine.common.serializer.JavaSerDeSerializer

class Message(val type: MessageType, val msg: ByteArray) {

    constructor(msg: WorkflowDispatched) : this(MessageType.WORKFLOW_DISPATCHED, JavaSerDeSerializer.serialize(msg))

    // Returns typed message, after having checked consistency
    inline fun <reified T> get(): T = when (T::class.qualifiedName) {
        type.className -> JavaSerDeSerializer.deserialize<T>(msg)
        else -> throw UnsupportedOperationException()
    }
}

enum class MessageType(val className: String?) {
    WORKFLOW_DISPATCHED(WorkflowDispatched::class.qualifiedName)
}
