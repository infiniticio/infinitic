package com.zenaton.engine.pulsar.messages

import com.zenaton.pulsar.workflows.PulsarMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec

class PulsarMessageTests : StringSpec({
    "PulsarMessage must allow a void constructor" {
        shouldNotThrowAny {
            PulsarMessage()
        }
    }
})
