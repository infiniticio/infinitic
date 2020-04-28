package com.zenaton.engine.pulsar.messages

import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec

class PulsarWorkflowMessageTests : StringSpec({
    "PulsarMessage must allow a void constructor" {
        shouldNotThrowAny {
            PulsarWorkflowMessage()
        }
    }
})
