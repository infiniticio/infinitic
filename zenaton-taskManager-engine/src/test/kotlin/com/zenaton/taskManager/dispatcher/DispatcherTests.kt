package com.zenaton.taskManager.dispatcher

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.engine.dispatcher.Dispatcher
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage
import com.zenaton.taskManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.taskManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.taskManager.common.messages.ForWorkerMessage
import com.zenaton.taskManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify

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
        verify {
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
        verify {
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
        verify {
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
        verify {
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
        verify {
            avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
        }
        confirmVerified(avroDispatcher)
    }
})
