package com.zenaton.jobManager.pulsar.functions

import com.zenaton.jobManager.functions.JobEngineFunction
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import com.zenaton.jobManager.pulsar.dispatcher.PulsarAvroDispatcher
import com.zenaton.jobManager.pulsar.storage.PulsarAvroStorage
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.apache.pulsar.functions.api.Context
import java.util.Optional

class JobEnginePulsarFunctionTests : StringSpec({
    "TaskEngineFunction should throw an exception if called without context" {
        // given
        val engine = JobEnginePulsarFunction()
        // then
        shouldThrowAny {
            engine.process(mockk<AvroForJobEngineMessage>(), null)
        }
    }

    "TaskEngineFunction should call engine with correct parameters" {
        // mocking
        val topicPrefixValue = mockk<Optional<Any>>()
        every { topicPrefixValue.isPresent } returns false
        val context = mockk<Context>()
        every { context.logger } returns mockk<org.slf4j.Logger>(relaxed = true)
        every { context.getUserConfigValue("topicPrefix") } returns topicPrefixValue
        val engineFunction = spyk(JobEngineFunction())
        every { engineFunction.handle(any()) } just Runs
        val avroMsg = mockk<AvroForJobEngineMessage>()
        // given
        val fct = JobEnginePulsarFunction()
        fct.engine = engineFunction
        // when
        fct.process(avroMsg, context)
        // then
        engineFunction.logger shouldBe context.logger
        (engineFunction.avroStorage as PulsarAvroStorage).context shouldBe context
        (engineFunction.avroDispatcher as PulsarAvroDispatcher).context shouldBe context
        verify(exactly = 1) { engineFunction.handle(avroMsg) }
    }
})
