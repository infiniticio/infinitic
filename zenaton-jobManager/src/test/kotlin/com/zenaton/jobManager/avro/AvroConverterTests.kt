package com.zenaton.jobManager.avro

import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.envelopes.ForEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage
import com.zenaton.jobManager.monitoringGlobal.MonitoringGlobalState
import com.zenaton.jobManager.monitoringPerName.MonitoringPerNameState
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AvroConverterTests : StringSpec({

    "messages should be avro-reversible" {
        Message::class.sealedSubclasses.forEach {
            println(it.qualifiedName)
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkerMessage) {
                    val avroMsg = AvroConverter.toWorkers(msg)
                    val msg2 = AvroConverter.fromWorkers(avroMsg)
                    val avroMsg2 = AvroConverter.toWorkers(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
                if (msg is ForEngineMessage) {
                    val avroMsg = AvroConverter.toEngine(msg)
                    val msg2 = AvroConverter.fromEngine(avroMsg)
                    val avroMsg2 = AvroConverter.toEngine(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
                if (msg is ForMonitoringPerNameMessage) {
                    val avroMsg = AvroConverter.toMonitoringPerName(msg)
                    val msg2 = AvroConverter.fromMonitoringPerName(avroMsg)
                    val avroMsg2 = AvroConverter.toMonitoringPerName(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
                if (msg is ForMonitoringGlobalMessage) {
                    val avroMsg = AvroConverter.toMonitoringGlobal(msg)
                    val msg2 = AvroConverter.fromMonitoringGlobal(avroMsg)
                    val avroMsg2 = AvroConverter.toMonitoringGlobal(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
            }
        }
    }

    "Engine state should be avro-reversible" {
        // given
        val state = TestFactory.get(EngineState::class)
        // when
        val avroState = AvroConverter.toAvro(state)
        val state2 = AvroConverter.fromAvro(avroState)
        val avroState2 = AvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "MonitoringPerName state state should be avro-reversible" {
        // given
        val state = TestFactory.get(MonitoringPerNameState::class)
        // when
        val avroState = AvroConverter.toAvro(state)
        val state2 = AvroConverter.fromAvro(avroState)
        val avroState2 = AvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "MonitoringGlobal state state should be avro-reversible" {
        // given
        val state = TestFactory.get(MonitoringGlobalState::class)
        // when
        val avroState = AvroConverter.toAvro(state)
        val state2 = AvroConverter.fromAvro(avroState)
        val avroState2 = AvroConverter.toAvro(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }
})
