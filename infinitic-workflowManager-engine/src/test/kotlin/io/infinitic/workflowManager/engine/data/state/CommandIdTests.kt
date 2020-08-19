package io.infinitic.workflowManager.engine.data.state

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.workflowManager.engine.data.commands.CommandId
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class CommandIdTests : StringSpec({
    "ActionId must create an uuid with a void constructor" {
        val actionId = CommandId(TaskId())
        shouldNotThrowAny {
            UUID.fromString(actionId.id)
        }
    }

    "ActionId must create a different uuid when called twice" {
        val id1 = CommandId(TaskId())
        val id2 = CommandId(TaskId())
        id1 shouldNotBe id2
    }
})
