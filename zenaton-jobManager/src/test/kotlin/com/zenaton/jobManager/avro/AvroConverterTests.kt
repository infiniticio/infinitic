package com.zenaton.jobManager.avro

import com.zenaton.jobManager.engine.JobEngineState
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.envelopes.ForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage
import com.zenaton.jobManager.monitoringGlobal.MonitoringGlobalState
import com.zenaton.jobManager.monitoringPerName.MonitoringPerNameState
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe

class AvroConverterTests : StringSpec({

    "Engine state should be avro-reversible" {
        // given
        val state = TestFactory.get(JobEngineState::class)
        // when
        val avroState = AvroConverter.toStorage(state)
        val state2 = AvroConverter.fromStorage(avroState)
        val avroState2 = AvroConverter.toStorage(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "MonitoringPerName state state should be avro-reversible" {
        // given
        val state = TestFactory.get(MonitoringPerNameState::class)
        // when
        val avroState = AvroConverter.toStorage(state)
        val state2 = AvroConverter.fromStorage(avroState)
        val avroState2 = AvroConverter.toStorage(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "MonitoringGlobal state state should be avro-reversible" {
        // given
        val state = TestFactory.get(MonitoringGlobalState::class)
        // when
        val avroState = AvroConverter.toStorage(state)
        val state2 = AvroConverter.fromStorage(avroState)
        val avroState2 = AvroConverter.toStorage(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    Message::class.sealedSubclasses.forEach {
        val msg = TestFactory.get(it)
        if (msg is ForWorkerMessage) {
            include(messagesToWorkersShouldBeAvroReversible(msg))
        }
        if (msg is ForJobEngineMessage) {
            include(messagesToJobEngineShouldBeAvroReversible(msg))
        }
        if (msg is ForMonitoringPerNameMessage) {
            include(messagesToMonitoringPerNameShouldBeAvroReversible(msg))
        }
        if (msg is ForMonitoringGlobalMessage) {
            include(messagesToMonitoringGlobalShouldBeAvroReversible(msg))
        }
    }
})

fun messagesToWorkersShouldBeAvroReversible(msg: ForWorkerMessage) = stringSpec {
    "${msg::class.simpleName!!} should be avro-convertible" {
        shouldNotThrowAny {
            val avroMsg = AvroConverter.toWorkers(msg)
            val msg2 = AvroConverter.fromWorkers(avroMsg)
            val avroMsg2 = AvroConverter.toWorkers(msg2)
            msg shouldBe msg2
            avroMsg shouldBe avroMsg2
        }
    }
}

fun messagesToJobEngineShouldBeAvroReversible(msg: ForJobEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} should be avro-convertible" {
        shouldNotThrowAny {
            val avroMsg = AvroConverter.toJobEngine(msg)
            val msg2 = AvroConverter.fromJobEngine(avroMsg)
            val avroMsg2 = AvroConverter.toJobEngine(msg2)
            msg shouldBe msg2
            avroMsg shouldBe avroMsg2
        }
    }
}

fun messagesToMonitoringPerNameShouldBeAvroReversible(msg: ForMonitoringPerNameMessage) = stringSpec {
    "${msg::class.simpleName!!} should be avro-convertible" {
        shouldNotThrowAny {
            val avroMsg = AvroConverter.toMonitoringPerName(msg)
            val msg2 = AvroConverter.fromMonitoringPerName(avroMsg)
            val avroMsg2 = AvroConverter.toMonitoringPerName(msg2)
            msg shouldBe msg2
            avroMsg shouldBe avroMsg2
        }
    }
}

fun messagesToMonitoringGlobalShouldBeAvroReversible(msg: ForMonitoringGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} should be avro-convertible" {
        shouldNotThrowAny {
            val avroMsg = AvroConverter.toMonitoringGlobal(msg)
            val msg2 = AvroConverter.fromMonitoringGlobal(avroMsg)
            val avroMsg2 = AvroConverter.toMonitoringGlobal(msg2)
            msg shouldBe msg2
            avroMsg shouldBe avroMsg2
        }
    }
}
