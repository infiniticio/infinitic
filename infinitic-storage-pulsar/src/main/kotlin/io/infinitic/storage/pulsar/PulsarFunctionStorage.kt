package io.infinitic.storage.pulsar

import io.infinitic.storage.api.Storage
import org.apache.pulsar.functions.api.Context
import java.nio.ByteBuffer

class PulsarFunctionStorage(private val context: Context) : Storage {
    override fun getState(key: String): ByteBuffer? = context.getState(key)

    override fun putState(key: String, value: ByteBuffer) = context.putState(key, value)

    override fun updateState(key: String, value: ByteBuffer) = context.putState(key, value)

    override fun deleteState(key: String) = context.deleteState(key)

    override fun incrementCounter(key: String, amount: Long) = context.incrCounter(key, amount)

    override fun getCounter(key: String): Long = context.getCounter(key)
}
