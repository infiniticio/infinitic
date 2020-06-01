package com.zenaton.taskManager.pulsar.engine

import com.zenaton.jobManager.engine.messages.AvroEngineMessage
import com.zenaton.taskManager.engine.Engine
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.pulsar.dispatcher.PulsarDispatcher
import com.zenaton.taskManager.pulsar.logger.PulsarLogger
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
        val msg = mockk<EngineMessage>()
        val avroMsg = mockk<AvroEngineMessage>()
        every { avroConverter.fromAvro(avroMsg) } returns msg
        // given
        val fct = EngineFunction()
        fct.taskEngine = taskEngine
        fct.avroConverter = avroConverter
        // when
        fct.process(avroMsg, context)
        // then
        (taskEngine.taskDispatcher as PulsarDispatcher).context shouldBe context
        (taskEngine.workflowDispatcher as WorkflowDispatcher).context shouldBe context
        (taskEngine.logger as PulsarLogger).context shouldBe context
        (taskEngine.storage as EnginePulsarStorage).context shouldBe context
        verify(exactly = 1) { taskEngine.handle(msg) }
    }

    "TaskEngineFunction should throw an exception if called without context" {
        // given
        val engine = EngineFunction()
        // then
        shouldThrowAny {
            engine.process(mockk<AvroEngineMessage>(), null)
        }
    }
})
