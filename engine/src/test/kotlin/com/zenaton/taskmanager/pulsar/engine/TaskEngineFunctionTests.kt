package com.zenaton.taskmanager.pulsar.engine

import com.zenaton.taskmanager.engine.TaskEngine
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.TaskMessage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.pulsar.logger.TaskLogger
import com.zenaton.taskmanager.pulsar.state.TaskStater
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
        val msg = mockk<TaskMessage>()
        val avroMsg = mockk<AvroTaskMessage>()
        every { avroConverter.fromAvro(avroMsg) } returns msg
        // given
        val fct = TaskEngineFunction()
        fct.taskEngine = taskEngine
        fct.avroConverter = avroConverter
        // when
        fct.process(avroMsg, context)
        // then
        (taskEngine.taskDispatcher as TaskDispatcher).context shouldBe context
        (taskEngine.workflowDispatcher as WorkflowDispatcher).context shouldBe context
        (taskEngine.logger as TaskLogger).context shouldBe context
        (taskEngine.stater as TaskStater).context shouldBe context
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
