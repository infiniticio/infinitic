package com.zenaton.engine.common.serializer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object JavaSerDeSerializer {
    fun <T> serialize(value: T): ByteArray {
        val bos = ByteArrayOutputStream()
        val out = ObjectOutputStream(bos)

        out.writeObject(value)
        out.flush()

        return bos.toByteArray()
    }

    inline fun <reified T> deserialize(data: ByteArray): T {
        val bis = ByteArrayInputStream(data)
        val ois = ObjectInputStream(bis)

        return ois.readObject() as T
    }
}
