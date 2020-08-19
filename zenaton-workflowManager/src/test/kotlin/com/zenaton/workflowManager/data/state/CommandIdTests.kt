package com.zenaton.workflowManager.data.state

import com.zenaton.taskManager.common.data.JobId
import com.zenaton.workflowManager.data.commands.CommandId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class CommandIdTests : StringSpec({
    "ActionId must create an uuid with a void constructor" {
        val actionId = CommandId(JobId())
        shouldNotThrowAny {
            UUID.fromString(actionId.id)
        }
    }

    "ActionId must create a different uuid when called twice" {
        val id1 = CommandId(JobId())
        val id2 = CommandId(JobId())
        id1 shouldNotBe id2
    }
})
