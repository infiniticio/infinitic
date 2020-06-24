package com.zenaton.jobManager.data

import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer

internal class JobMetaBuilderTest : StringSpec({
    "JobMetaBuiler should build correct JobMeta" {
        // given
        val str = TestFactory.random(String::class)
        val bytes = TestFactory.random(ByteArray::class)
        val buffer = TestFactory.random(ByteBuffer::class)
        // when
        val out = JobMeta
            .builder()
            .add("key1", str)
            .add("key2", bytes)
            .add("key3", buffer)
            .build()
        // then
        out.meta.size shouldBe 3
        out.meta["key1"] shouldBe str.toByteArray(Charsets.UTF_8)
        out.meta["key2"] shouldBe bytes
        ByteBuffer.wrap(out.meta["key3"]) shouldBe buffer
    }

    "JobMetaBuiler should not accept duplicate keys" {
        // given
        val str = TestFactory.random(String::class)
        // when
        shouldThrowAny {
            JobMeta
                .builder()
                .add("key", str)
                .add("key", str)
                .build()
        }
    }
})
