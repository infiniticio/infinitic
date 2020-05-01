package com.zenaton.pulsar.utils

import java.nio.ByteBuffer

interface StateSerDeInterface {
    fun <T> serialize(state: T): ByteBuffer
    fun <T> deserialize(data: ByteBuffer): T
}
