package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.common.messages.ForJobEngineMessage
import com.zenaton.jobManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.jobManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.jobManager.common.messages.ForWorkerMessage
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify

class DispatcherTests : StringSpec({
    val avroDispatcher = mockk<AvroDispatcher>(relaxed = true)
    val dispatcher = Dispatcher(avroDispatcher)

    "Dispatcher.toJobEngine should send correct parameter" {
        // given
        val msg = TestFactory.random(ForJobEngineMessage::class)
        val after = TestFactory.random(Float::class)
        // when
        dispatcher.toJobEngine(msg, after)
        // then
        verify {
            avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg), after)
        }
        confirmVerified(avroDispatcher)
    }

    "Dispatcher.toJobEngine should send correct default after parameter" {
        // given
        val msg = TestFactory.random(ForJobEngineMessage::class)
        // when
        dispatcher.toJobEngine(msg)
        // then
        verify {
            avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg), 0F)
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
