package com.zenaton.jobManager.data

import java.lang.Exception
import java.nio.ByteBuffer

class JobMetaBuilder {
    private var meta: MutableMap<String, ByteArray> = mutableMapOf()

    fun add(key: String, data: String): JobMetaBuilder {
        checkKey(key)
        meta[key] = data.toByteArray(Charsets.UTF_8)

        return this
    }

    fun add(key: String, data: ByteArray): JobMetaBuilder {
        checkKey(key)
        meta[key] = data

        return this
    }

    fun add(key: String, data: ByteBuffer): JobMetaBuilder {
        val p = data.position()
        val bytes = ByteArray(data.remaining())
        data.get(bytes, 0, bytes.size)
        data.position(p)

        return add(key, bytes)
    }

    fun build() = JobMeta(meta)

    private fun checkKey(key: String) {
        if (meta.containsKey(key)) throw Exception("Duplicated $key key")
    }
}
