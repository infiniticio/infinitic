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

import io.infinitic.common.metrics.global.messages.GlobalMetricsEnvelope
import io.infinitic.common.metrics.global.messages.GlobalMetricsMessage
import io.infinitic.metrics.global.engine.MetricsGlobalEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.apache.pulsar.functions.api.Context

class MetricsGlobalPulsarFunctionTests : ShouldSpec({
    context("MonitoringGlobalPulsarFunction.process") {

        should("throw an exception if called with a null context") {
            val function = MetricsGlobalPulsarFunction()

            shouldThrow<NullPointerException> {
                function.process(mockk(), null)
            }
        }

        should("call metrics with correct parameters") {
            // mocking Context
            val context = mockk<Context>()
            every { context.logger } returns mockk()

            // Mocking avro conversion
            val envelope = mockk<GlobalMetricsEnvelope>()
            val msg = mockk<GlobalMetricsMessage>()
            every { envelope.message() } returns msg

            // Mocking Task Engine
            val metricsGlobal = mockk<MetricsGlobalEngine>()
            val metricsGlobalPulsarFunction = spyk<MetricsGlobalPulsarFunction>()
            every { metricsGlobalPulsarFunction.getMetricsGlobalEngine(context) } returns metricsGlobal
            coEvery { metricsGlobal.handle(msg) } just Runs

            // when
            metricsGlobalPulsarFunction.process(envelope, context)
            // then
            coVerify(exactly = 1) { metricsGlobal.handle(msg) }
            unmockkAll()
        }
    }
})
