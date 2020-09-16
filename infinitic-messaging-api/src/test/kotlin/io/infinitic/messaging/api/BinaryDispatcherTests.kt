package io.infinitic.messaging.api

import io.infinitic.messaging.api.dispatcher.BinaryDispatcher
import io.infinitic.messaging.api.dispatcher.transport.BinaryCompatibleTransport
import io.infinitic.messaging.api.utils.TestFactory
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk

class BinaryDispatcherTests : StringSpec({
    val transport = mockk<BinaryCompatibleTransport>(relaxed = true)
    val dispatcher = BinaryDispatcher(transport)

    "BinaryDispatcher.toTaskEngine should send correct parameter" {
        // given
        val msg = TestFactory.random(ForTaskEngineMessage::class)
        val after = TestFactory.random(Float::class)
        // when
        dispatcher.toTaskEngine(msg, after)
        // then
        coVerify {
            transport.toTaskEngine(ofType(), after)
        }
        confirmVerified(transport)
    }

    "BinaryDispatcher.toTaskEngine should send correct default after parameter" {
        // given
        val msg = TestFactory.random(ForTaskEngineMessage::class)
        // when
        dispatcher.toTaskEngine(msg)
        // then
        coVerify {
            transport.toTaskEngine(ofType(), 0F)
        }
        confirmVerified(transport)
    }

    "BinaryDispatcher.toMonitoringPerName should send correct parameter" {
        // given
        val msg = TestFactory.random(ForMonitoringPerNameMessage::class)
        // when
        dispatcher.toMonitoringPerName(msg)
        // then
        coVerify {
            transport.toMonitoringPerName(ofType())
        }
        confirmVerified(transport)
    }
    "BinaryDispatcher.toMonitoringGlobal should send correct parameter" {
        // given
        val msg = TestFactory.random(ForMonitoringGlobalMessage::class)
        // when
        dispatcher.toMonitoringGlobal(msg)
        // then
        coVerify {
            transport.toMonitoringGlobal(ofType())
        }
        confirmVerified(transport)
    }

    "BinaryDispatcher.toWorkers should send correct parameter" {
        // given
        val msg = TestFactory.random(ForWorkerMessage::class)
        // when
        dispatcher.toWorkers(msg)
        // then
        coVerify {
            transport.toWorkers(ofType())
        }
        confirmVerified(transport)
    }
})
