package com.zenaton.engine.workflows.state

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class UnitStepIdTests : StringSpec({
    "UnitStepId must create an uuid with a void constructor" {
        val id = UnitStepId()
        shouldNotThrowAny {
            UUID.fromString(id.uuid)
        }
    }

    "UnitStepId must create a different uuid when called twice" {
        val id1 = UnitStepId()
        val id2 = UnitStepId()
        id1 shouldNotBe id2
    }
})
