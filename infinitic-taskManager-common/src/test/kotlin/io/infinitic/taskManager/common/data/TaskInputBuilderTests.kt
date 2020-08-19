package io.infinitic.taskManager.common.data

import io.infinitic.taskManager.states.AvroTaskEngineState
import io.infinitic.taskManager.common.states.TaskEngineState
import io.infinitic.taskManager.common.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer

internal class TaskInputBuilderTests : StringSpec({
    "TaskInputBuilder should build correct TaskInput" {
        // given
        val state = TestFactory.random(TaskEngineState::class)
        val bytes = TestFactory.random(ByteArray::class)
        val buffer = TestFactory.random(ByteBuffer::class)
        val avro = TestFactory.random(AvroTaskEngineState::class)
        // when
        val out = TaskInput.builder()
            .add(null)
            .add(state)
            .add(bytes)
            .add(buffer)
            .add(avro)
            .build()
        // then
        val input = out.input
        input.size shouldBe 5
        input[0].deserialize() shouldBe null
        input[1].deserialize() shouldBe state
        input[2].deserialize() shouldBe bytes
        input[3].deserialize() shouldBe buffer.array()
        input[4].deserialize() shouldBe avro
    }
})
