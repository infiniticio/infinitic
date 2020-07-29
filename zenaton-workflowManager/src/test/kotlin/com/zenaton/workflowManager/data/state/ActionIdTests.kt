package com.zenaton.workflowManager.data.state

import com.zenaton.jobManager.common.data.JobId
import com.zenaton.workflowManager.data.actions.ActionId
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
