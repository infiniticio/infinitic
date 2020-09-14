package io.infinitic.storage.api

import java.nio.ByteBuffer

interface StateStorage {
    fun getState(key: String): ByteBuffer?
    fun putState(key: String, value: ByteBuffer)
    fun updateState(key: String, value: ByteBuffer)
    fun deleteState(key: String)
}
