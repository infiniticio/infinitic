package io.infinitic.storage.api

interface CounterStorage {
    fun incrementCounter(key: String, amount: Long)
    fun getCounter(key: String): Long
}
