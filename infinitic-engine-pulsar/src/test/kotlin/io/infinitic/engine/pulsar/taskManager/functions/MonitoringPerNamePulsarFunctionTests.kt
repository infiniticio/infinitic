// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engine.pulsar.taskManager.functions

import io.infinitic.common.serDe.avro.AvroConverter
import io.infinitic.engine.monitoringPerName.engine.MonitoringPerName
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameMessage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
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

class MonitoringPerNamePulsarFunctionTests : ShouldSpec({
    context("MonitoringPerNamePulsarFunctionTests.process") {

        should("throw an exception if called with a null context") {
            val function = MonitoringPerNamePulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking Context
            val context = mockk<Context>()
            every { context.logger } returns mockk()

            // Mocking avro conversion
            val avroMsg = mockk<AvroEnvelopeForMonitoringPerName>()
            val msg = mockk<MonitoringPerNameMessage>()
            mockkObject(AvroConverter)
            every { AvroConverter.fromMonitoringPerName(avroMsg) } returns msg

            // Mocking Monitoring Per Name
            val monitoringPerName = mockk<MonitoringPerName>()
            val monitoringPerNamePulsarFunction = spyk<MonitoringPerNamePulsarFunction>()
            every { monitoringPerNamePulsarFunction.getMonitoringPerName(context) } returns monitoringPerName
            coEvery { monitoringPerName.handle(msg) } just Runs

            // when
            monitoringPerNamePulsarFunction.process(avroMsg, context)
            // then
            coVerify(exactly = 1) { monitoringPerName.handle(msg) }
            unmockkAll()
        }
    }
})
