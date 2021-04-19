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

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk

class TaskEnginePulsarFunctionTests : StringSpec({
    "TaskEnginePulsarFunction should throw an exception if called without context" {
        // given
        val engine = TaskEnginePulsarFunction()
        // then
        shouldThrowAny {
            engine.process(mockk(), null)
        }
    }

//    "TaskEnginePulsarFunction should call engine with correct parameters" {
//        // mocking Pulsar Context
//        val context = mockk<Context>()
//        every { context.logger } returns mockk()
//
//        // Mocking avro conversion
//        val envelope = mockk<TaskEngineEnvelope>()
//        val msg = mockk<TaskEngineMessage>()
//        every { envelope.message() } returns msg
//
//        // Mocking Task Engine
//        val taskEngine = mockk<TaskEngine>()
//        val taskEnginePulsarFunction = spyk<TaskEnginePulsarFunction>()
//        every { taskEnginePulsarFunction.getTaskEngine(context) } returns taskEngine
//        coEvery { taskEngine.handle(msg) } just Runs
//
//        // when
//        taskEnginePulsarFunction.process(envelope, context)
//        // then
//        coVerify(exactly = 1) { taskEngine.handle(msg) }
//
//        unmockkAll()
//    }
})
