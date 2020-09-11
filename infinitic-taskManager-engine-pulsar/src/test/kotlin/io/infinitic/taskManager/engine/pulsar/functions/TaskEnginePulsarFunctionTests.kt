package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
import java.util.Optional

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
        // mocking
        val topicPrefixValue = mockk<Optional<Any>>()
        every { topicPrefixValue.isPresent } returns false
        val context = mockk<Context>()
        every { context.logger } returns mockk(relaxed = true)
        every { context.getUserConfigValue("topicPrefix") } returns topicPrefixValue
        val engineFunction = spyk(TaskEngine())
        coEvery { engineFunction.handle(any()) } just Runs
        val avroMsg = mockk<AvroEnvelopeForTaskEngine>()
        mockkObject(AvroConverter)
        every { AvroConverter.fromTaskEngine(any()) } returns mockk()
        // given
        val fct = TaskEnginePulsarFunction()
        fct.engine = engineFunction
        // when
        fct.process(avroMsg, context)
        // then
        engineFunction.logger shouldBe context.logger
        coVerify(exactly = 1) { engineFunction.handle(any()) }
        unmockkAll()
    }
})
