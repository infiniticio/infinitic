package io.infinitic.workflowManager.engine.pulsar.functions

import io.infinitic.workflowManager.engine.avroEngines.AvroWorkflowEngine
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.workflowManager.pulsar.functions.WorkflowEnginePulsarFunction
import io.infinitic.workflowManager.pulsar.storage.PulsarAvroStorage
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
        // mocking
        val context = mockk<Context>()
        every { context.logger } returns mockk<org.slf4j.Logger>(relaxed = true)
        val engineFunction = spyk(AvroWorkflowEngine())
        every { engineFunction.handle(any()) } just Runs
        val avroMsg = mockk<AvroEnvelopeForWorkflowEngine>()
        // given
        val fct = WorkflowEnginePulsarFunction()
        fct.engine = engineFunction
        // when
        fct.process(avroMsg, context)
        // then
        engineFunction.logger shouldBe context.logger
        (engineFunction.avroStorage as PulsarAvroStorage).context shouldBe context
        verify(exactly = 1) { engineFunction.handle(avroMsg) }
    }
})
