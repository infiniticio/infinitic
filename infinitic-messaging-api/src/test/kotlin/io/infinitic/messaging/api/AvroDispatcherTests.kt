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

package io.infinitic.messaging.api

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.messaging.api.dispatcher.AvroDispatcher
import io.infinitic.messaging.api.dispatcher.transport.AvroTransport
import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk

class AvroDispatcherTests : StringSpec({
    val transport = mockk<AvroTransport>(relaxed = true)
    val dispatcher = AvroDispatcher(transport)

    "AvroDispatcher.toTaskEngine should send correct parameter" {
        // given
        val msg = TestFactory.random(ForTaskEngineMessage::class)
        val after = TestFactory.random(Float::class)
        // when
        dispatcher.toTaskEngine(msg, after)
        // then
        coVerify {
            transport.toTaskEngine(AvroConverter.toTaskEngine(msg), after)
        }
        confirmVerified(transport)
    }

    "AvroDispatcher.toTaskEngine should send correct default after parameter" {
        // given
        val msg = TestFactory.random(ForTaskEngineMessage::class)
        // when
        dispatcher.toTaskEngine(msg)
        // then
        coVerify {
            transport.toTaskEngine(AvroConverter.toTaskEngine(msg), 0F)
        }
        confirmVerified(transport)
    }

    "AvroDispatcher.toMonitoringPerName should send correct parameter" {
        // given
        val msg = TestFactory.random(ForMonitoringPerNameMessage::class)
        // when
        dispatcher.toMonitoringPerName(msg)
        // then
        coVerify {
            transport.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
        }
        confirmVerified(transport)
    }

    "AvroDispatcher.toMonitoringGlobal should send correct parameter" {
        // given
        val msg = TestFactory.random(ForMonitoringGlobalMessage::class)
        // when
        dispatcher.toMonitoringGlobal(msg)
        // then
        coVerify {
            transport.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
        }
        confirmVerified(transport)
    }

    "AvroDispatcher.toWorkers should send correct parameter" {
        // given
        val msg = TestFactory.random(ForWorkerMessage::class)
        // when
        dispatcher.toWorkers(msg)
        // then
        coVerify {
            transport.toWorkers(AvroConverter.toWorkers(msg))
        }
        confirmVerified(transport)
    }
})
