package com.zenaton.engine.pulsar.messages

import com.zenaton.pulsar.workflows.PulsarMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MessageTests : StringSpec({
    beforeTest { println("Starting a test $it") }

    "Message must allow a void constructor" {
        shouldNotThrowAny {
            PulsarMessage()
        }
    }

    "strings.length should return size of string" {
        "hello".length shouldBe 5
    }
})
