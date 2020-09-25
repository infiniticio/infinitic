package io.infinitic.workflowManager.engine.pulsar.functions

import io.infinitic.common.workflowManager.avro.AvroConverter
import io.infinitic.common.workflowManager.messages.ForWorkflowEngineMessage
import io.infinitic.workflowManager.engine.engines.WorkflowEngine
import io.infinitic.avro.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.workflowManager.pulsar.functions.WorkflowEnginePulsarFunction
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

class WorkflowEnginePulsarFunctionTests : StringSpec({
    "WorkflowEnginePulsarFunction should throw an exception if called without context" {
        // given
        val engine = WorkflowEnginePulsarFunction()
        // then
        shouldThrowAny {
            engine.process(mockk<AvroEnvelopeForWorkflowEngine>(), null)
        }
    }

    "WorkflowEnginePulsarFunction should call engine with correct parameters" {
        // mocking Pulsar Context
        val context = mockk<Context>()
        every { context.logger } returns mockk()

        // Mocking avro conversion
        val avroMsg = mockk<AvroEnvelopeForWorkflowEngine>()
        val msg = mockk<ForWorkflowEngineMessage>()
        mockkObject(AvroConverter)
        every { AvroConverter.fromWorkflowEngine(avroMsg) } returns msg

        // Mocking Task Engine
        val workflowEngine = mockk<WorkflowEngine>()
        val workflowEnginePulsarFunction = spyk<WorkflowEnginePulsarFunction>()
        every { workflowEnginePulsarFunction.getWorkflowEngine(context) } returns workflowEngine
        coEvery { workflowEngine.handle(msg) } just Runs

        // when
        workflowEnginePulsarFunction.process(avroMsg, context)
        // then
        coVerify(exactly = 1) { workflowEngine.handle(msg) }

        unmockkAll()
    }
})
