/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar.functions

import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk

class WorkflowEnginePulsarFunctionTests : StringSpec({
    "WorkflowEnginePulsarFunction should throw an exception if called without context" {
        // given
        val engine = WorkflowEnginePulsarFunction(WorkflowName(("Test")))
        // then
        shouldThrowAny {
            engine.process(mockk<WorkflowEngineEnvelope>(), null)
        }
    }

//    "WorkflowEnginePulsarFunction should call engine with correct parameters" {
//        // mocking Pulsar Context
//        val context = mockk<Context>()
//        every { context.logger } returns mockk()
//
//        // Mocking avro conversion
//        val envelope = mockk<WorkflowEngineEnvelope>()
//        val msg = mockk<WorkflowEngineMessage>()
//        every { envelope.message() } returns msg
//
//        // Mocking Task Engine
//        val workflowEngine = mockk<WorkflowEngine>()
//        val workflowEnginePulsarFunction = spyk<WorkflowEnginePulsarFunction>()
//        every { workflowEnginePulsarFunction.getWorkflowEngine(context) } returns workflowEngine
//        coEvery { workflowEngine.handle(msg) } just Runs
//
//        // when
//        workflowEnginePulsarFunction.process(envelope, context)
//        // then
//        coVerify(exactly = 1) { workflowEngine.handle(msg) }
//
//        unmockkAll()
//    }
})
