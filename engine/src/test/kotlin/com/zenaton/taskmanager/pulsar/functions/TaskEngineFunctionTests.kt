package com.zenaton.taskmanager.pulsar.functions

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.taskmanager.engine.TaskEngine
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.stater.TaskStater
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

class TaskEngineFunctionTests : StringSpec({
    "TaskEngineFunction should call engine with correct parameters" {
        // mocking
        val context = mockk<Context>()
        every { context.logger } returns mockk<org.slf4j.Logger>()
        val taskEngine = spyk(TaskEngine())
        every { taskEngine.handle(any()) } just Runs
        val avroConverter = mockk<TaskAvroConverter>()
        val msg = mockk<TaskMessageInterface>()
        val avroMsg = mockk<AvroTaskMessage>()
        every { avroConverter.fromAvro(avroMsg) } returns msg
        // given
        val fct = TaskEngineFunction()
        fct.taskEngine = taskEngine
        fct.avroConverter = avroConverter
        // when
        fct.process(avroMsg, context)
        // then
        (taskEngine.stater as TaskStater).context shouldBe context
        (taskEngine.dispatcher as TaskEngineDispatcher).context shouldBe context
        (taskEngine.logger as Logger).context shouldBe context
        verify(exactly = 1) { taskEngine.handle(msg) }
    }

    "TaskEngineFunction should throw an exception if called without context" {
        // given
        val engine = TaskEngineFunction()
        // then
        shouldThrowAny {
            engine.process(mockk<AvroTaskMessage>(), null)
        }
    }
})
