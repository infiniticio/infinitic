package com.zenaton.taskmanager.pulsar.engine

import com.zenaton.taskmanager.engine.TaskEngine
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessage
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.dispatcher.PulsarTaskDispatcher
import com.zenaton.taskmanager.pulsar.logger.PulsarTaskLogger
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

class TaskEngineFunctionTests : StringSpec({
    "TaskEngineFunction should call engine with correct parameters" {
        // mocking
        val context = mockk<Context>()
        every { context.logger } returns mockk<org.slf4j.Logger>()
        val taskEngine = spyk(TaskEngine())
        every { taskEngine.handle(any()) } just Runs
        val avroConverter = mockk<TaskAvroConverter>()
        val msg = mockk<TaskEngineMessage>()
        val avroMsg = mockk<AvroTaskEngineMessage>()
        every { avroConverter.fromAvro(avroMsg) } returns msg
        // given
        val fct = TaskEngineFunction()
        fct.taskEngine = taskEngine
        fct.avroConverter = avroConverter
        // when
        fct.process(avroMsg, context)
        // then
        (taskEngine.taskDispatcher as PulsarTaskDispatcher).context shouldBe context
        (taskEngine.workflowDispatcher as WorkflowDispatcher).context shouldBe context
        verify(exactly = 1) { taskEngine.handle(msg) }
    }

    "TaskEngineFunction should throw an exception if called without context" {
        // given
        val engine = TaskEngineFunction()
        // then
        shouldThrowAny {
            engine.process(mockk<AvroTaskEngineMessage>(), null)
        }
    }
})
