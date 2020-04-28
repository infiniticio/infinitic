package com.zenaton.pulsar.topics.workflows.serializers

import com.zenaton.engine.data.workflows.WorkflowState
import java.nio.ByteBuffer

interface StateSerDeInterface {
    fun serialize(state: WorkflowState): ByteBuffer
    fun deserialize(data: ByteBuffer): WorkflowState
}
