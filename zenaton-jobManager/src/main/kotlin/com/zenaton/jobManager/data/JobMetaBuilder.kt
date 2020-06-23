package com.zenaton.jobManager.data

import java.nio.ByteBuffer

class JobMetaBuilder {
    private var meta : MutableMap<String, ByteArray> = mutableMapOf()

    fun add(key: String, data: String): JobMetaBuilder {
        meta.put(key, data.toByteArray(Charsets.UTF_8))

        return this
    }

    fun get() = JobMeta(meta)
}
