package com.zenaton.pulsar.serializer

import com.zenaton.engine.workflows.WorkflowState
import com.zenaton.engine.workflows.messages.WorkflowMessage
import com.zenaton.pulsar.workflows.PulsarMessage
import java.nio.ByteBuffer

interface MessageSerDeInterface {
    fun deSerializeState(byteBuffer: ByteBuffer): WorkflowState
    fun serializeState(state: WorkflowState): ByteBuffer
    fun fromPulsar(input: PulsarMessage): WorkflowMessage
    fun toPulsar(msg: WorkflowMessage): PulsarMessage
}
