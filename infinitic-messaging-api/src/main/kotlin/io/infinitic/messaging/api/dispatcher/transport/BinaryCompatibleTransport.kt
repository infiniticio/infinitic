package io.infinitic.messaging.api.dispatcher.transport

import java.nio.ByteBuffer

interface BinaryCompatibleTransport {
    suspend fun toWorkers(msg: ByteBuffer)
    suspend fun toTaskEngine(msg: ByteBuffer, after: Float = 0f)
    suspend fun toMonitoringGlobal(msg: ByteBuffer)
    suspend fun toMonitoringPerName(msg: ByteBuffer)
}
