package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.workflows.WorkflowState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer

object StateSerDe : StateSerDeInterface {

    override fun serialize(state: WorkflowState): ByteBuffer {
        val bos = ByteArrayOutputStream()
        val out = ObjectOutputStream(bos)

        out.writeObject(state)
        out.flush()

        return ByteBuffer.wrap(bos.toByteArray())
    }

    override fun  deserialize(data: ByteBuffer): WorkflowState {
        val bis = ByteArrayInputStream(data.array())
        val ois = ObjectInputStream(bis)

        return ois.readObject() as WorkflowState
    }
}
