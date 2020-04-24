package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.data.workflows.WorkflowState
import java.nio.ByteBuffer

interface StateSerDeInterface {
    fun serialize(state: WorkflowState): ByteBuffer
    fun deserialize(data: ByteBuffer): WorkflowState
}
