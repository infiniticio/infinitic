package com.zenaton.jobManager.pulsar.engine

import com.zenaton.jobManager.engine.Engine
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.pulsar.avro.AvroConverter
import com.zenaton.jobManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
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

class EngineFunctionTests : StringSpec({
    "TaskEngineFunction should call engine with correct parameters" {
        // mocking
        val context = mockk<Context>()
        every { context.logger } returns mockk<org.slf4j.Logger>()
        val taskEngine = spyk(Engine())
        every { taskEngine.handle(any()) } just Runs
        val avroConverter = mockk<AvroConverter>()
        val msg = mockk<ForEngineMessage>()
        val avroMsg = mockk<AvroForEngineMessage>()
        every { avroConverter.fromAvroForEngineMessage(avroMsg) } returns msg
        // given
        val fct = EngineFunction()
        fct.taskEngine = taskEngine
        fct.avroConverter = avroConverter
        // when
        fct.process(avroMsg, context)
        // then
        (taskEngine.dispatch as PulsarDispatcher).context shouldBe context
        (taskEngine.workflowDispatcher as WorkflowDispatcher).context shouldBe context
        taskEngine.logger shouldBe context.logger
        (taskEngine.storage as EnginePulsarStorage).context shouldBe context
        verify(exactly = 1) { taskEngine.handle(msg) }
    }

    "TaskEngineFunction should throw an exception if called without context" {
        // given
        val engine = EngineFunction()
        // then
        shouldThrowAny {
            engine.process(mockk<AvroForEngineMessage>(), null)
        }
    }
})