package io.infinitic.messaging.api

import io.infinitic.messaging.api.dispatcher.AvroDispatcher
import io.infinitic.messaging.api.dispatcher.transport.AvroCompatibleTransport
import io.infinitic.messaging.api.utils.TestFactory
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
    val transport = mockk<AvroCompatibleTransport>(relaxed = true)
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
