package io.infinitic.engine.pulsar.taskManager.functions

import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import org.apache.pulsar.functions.api.Context

class TaskEnginePulsarFunctionTests : StringSpec({
    "TaskEnginePulsarFunction should throw an exception if called without context" {
        // given
        val engine = TaskEnginePulsarFunction()
        // then
        shouldThrowAny {
            engine.process(mockk(), null)
        }
    }

    "TaskEnginePulsarFunction should call engine with correct parameters" {
        // mocking Pulsar Context
        val context = mockk<Context>()
        every { context.logger } returns mockk()

        // Mocking avro conversion
        val avroMsg = mockk<AvroEnvelopeForTaskEngine>()
        val msg = mockk<ForTaskEngineMessage>()
        mockkObject(AvroConverter)
        every { AvroConverter.fromTaskEngine(avroMsg) } returns msg

        // Mocking Task Engine
        val taskEngine = mockk<TaskEngine>()
        val taskEnginePulsarFunction = spyk<TaskEnginePulsarFunction>()
        every { taskEnginePulsarFunction.getTaskEngine(context) } returns taskEngine
        coEvery { taskEngine.handle(msg) } just Runs

        // when
        taskEnginePulsarFunction.process(avroMsg, context)
        // then
        coVerify(exactly = 1) { taskEngine.handle(msg) }

        unmockkAll()
    }
})
