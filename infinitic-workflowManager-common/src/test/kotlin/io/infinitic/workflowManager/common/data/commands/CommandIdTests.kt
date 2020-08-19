package io.infinitic.workflowManager.common.data.commands

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.common.data.commands.CommandId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class CommandIdTests : StringSpec({
    "CommandId must create an uuid with a void constructor" {
        val actionId = CommandId(TaskId())
        shouldNotThrowAny {
            UUID.fromString(actionId.id)
        }
    }

    "CommandId must create a different uuid when called twice" {
        val id1 = CommandId(TaskId())
        val id2 = CommandId(TaskId())
        id1 shouldNotBe id2
    }
})
