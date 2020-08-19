package com.zenaton.taskManager.common.data

import com.zenaton.taskManager.common.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer

internal class JobMetaBuilderTests : StringSpec({
    "JobMetaBuiler should build correct JobMeta" {
        // given
        val str = TestFactory.random(String::class)
        val bytes = TestFactory.random(ByteArray::class)
        val buffer = TestFactory.random(ByteBuffer::class)
        // when
        val out = JobMeta.builder()
            .add("key1", str)
            .add("key2", bytes)
            .add("key3", buffer)
            .build()
        // then
        out.meta.size shouldBe 3
        out.meta["key1"]?.deserialize() shouldBe str
        out.meta["key2"]?.deserialize() shouldBe bytes
        out.meta["key3"]?.deserialize() shouldBe buffer.array()
    }
})
