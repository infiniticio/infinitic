package io.infinitic.taskManager.engine.dispatcher.transport

import java.nio.ByteBuffer

interface BinaryTransport {
    suspend fun toWorkers(msg: ByteBuffer)
    suspend fun toTaskEngine(msg: ByteBuffer, after: Float = 0f)
    suspend fun toMonitoringGlobal(msg: ByteBuffer)
    suspend fun toMonitoringPerName(msg: ByteBuffer)
}
