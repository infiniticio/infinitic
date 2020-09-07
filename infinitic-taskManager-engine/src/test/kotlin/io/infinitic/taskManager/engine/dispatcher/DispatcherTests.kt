package io.infinitic.taskManager.engine.dispatcher

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage
import io.infinitic.taskManager.engine.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk

class DispatcherTests : StringSpec({
    val avroDispatcher = mockk<AvroDispatcher>(relaxed = true)
    val dispatcher = Dispatcher(avroDispatcher)

    "Dispatcher.toTaskEngine should send correct parameter" {
        // given
        val msg = TestFactory.random(ForTaskEngineMessage::class)
        val after = TestFactory.random(Float::class)
        // when
        dispatcher.toTaskEngine(msg, after)
        // then
        coVerify {
            avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg), after)
        }
        confirmVerified(avroDispatcher)
    }

    "Dispatcher.toTaskEngine should send correct default after parameter" {
        // given
        val msg = TestFactory.random(ForTaskEngineMessage::class)
        // when
        dispatcher.toTaskEngine(msg)
        // then
        coVerify {
            avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg), 0F)
        }
        confirmVerified(avroDispatcher)
    }

    "Dispatcher.toMonitoringPerName should send correct parameter" {
        // given
        val msg = TestFactory.random(ForMonitoringPerNameMessage::class)
        // when
        dispatcher.toMonitoringPerName(msg)
        // then
        coVerify {
            avroDispatcher.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
        }
        confirmVerified(avroDispatcher)
    }

    "Dispatcher.toMonitoringGlobal should send correct parameter" {
        // given
        val msg = TestFactory.random(ForMonitoringGlobalMessage::class)
        // when
        dispatcher.toMonitoringGlobal(msg)
        // then
        coVerify {
            avroDispatcher.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
        }
        confirmVerified(avroDispatcher)
    }

    "Dispatcher.toWorkers should send correct parameter" {
        // given
        val msg = TestFactory.random(ForWorkerMessage::class)
        // when
        dispatcher.toWorkers(msg)
        // then
        coVerify {
            avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
        }
        confirmVerified(avroDispatcher)
    }
})
