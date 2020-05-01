package com.zenaton.pulsar.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer

object StateSerDe : StateSerDeInterface {
    // TODO This class needs to be refactored to handle schema evolution, eg. using AVRO

    override fun <T> serialize(state: T): ByteBuffer {
        val bos = ByteArrayOutputStream()
        val out = ObjectOutputStream(bos)

        out.writeObject(state)
        out.flush()

        return ByteBuffer.wrap(bos.toByteArray())
    }

    override fun <T> deserialize(data: ByteBuffer): T {
        val bis = ByteArrayInputStream(data.array())
        val ois = ObjectInputStream(bis)

        return ois.readObject() as T
    }
}
