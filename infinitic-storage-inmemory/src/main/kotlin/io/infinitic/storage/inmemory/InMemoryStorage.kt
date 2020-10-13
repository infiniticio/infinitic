package io.infinitic.storage.inmemory

import io.infinitic.storage.api.Flushable
import io.infinitic.storage.api.Storage
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

class InMemoryStorage internal constructor() : Storage, Flushable {
    private val stateStorage = ConcurrentHashMap<String, ByteBuffer>()
    private val counterStorage = ConcurrentHashMap<String, LongAdder>()

    override fun getState(key: String): ByteBuffer? = stateStorage[key]

    override fun putState(key: String, value: ByteBuffer) {
        stateStorage[key] = value
    }

    override fun updateState(key: String, value: ByteBuffer) = putState(key, value)

    override fun deleteState(key: String) {
        stateStorage.remove(key)
    }

    override fun incrementCounter(key: String, amount: Long) = counterStorage.computeIfAbsent(key) { LongAdder() }.add(amount)

    override fun getCounter(key: String): Long = counterStorage.computeIfAbsent(key) { LongAdder() }.sum()

    override fun flush() {
        stateStorage.clear()
        counterStorage.clear()
    }
}

fun inMemory(): InMemoryStorage = InMemoryStorage()
