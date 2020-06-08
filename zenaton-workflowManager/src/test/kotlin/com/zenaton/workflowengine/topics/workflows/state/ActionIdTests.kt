package com.zenaton.workflowengine.topics.workflows.state

import com.zenaton.jobManager.data.JobId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class ActionIdTests : StringSpec({
    "ActionId must create an uuid with a void constructor" {
        val actionId = ActionId(JobId())
        shouldNotThrowAny {
            UUID.fromString(actionId.id)
        }
    }

    "ActionId must create a different uuid when called twice" {
        val id1 = ActionId(JobId())
        val id2 = ActionId(JobId())
        id1 shouldNotBe id2
    }
})
