package com.zenaton.jobManager.pulsar.avro

import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalState
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AvroConverterTests : StringSpec({

    "messages should be avro-reversible" {
        Message::class.sealedSubclasses.forEach {
            shouldNotThrowAny {
                val msg = TestFactory.get(it)
                if (msg is ForWorkerMessage) {
                    val avroMsg = AvroConverter.toAvroForWorkerMessage(msg)
                    val msg2 = AvroConverter.fromAvroForWorkerMessage(avroMsg)
                    val avroMsg2 = AvroConverter.toAvroForWorkerMessage(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
                if (msg is ForEngineMessage) {
                    val avroMsg = AvroConverter.toAvroForEngineMessage(msg)
                    val msg2 = AvroConverter.fromAvroForEngineMessage(avroMsg)
                    val avroMsg2 = AvroConverter.toAvroForEngineMessage(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
                if (msg is ForMonitoringPerNameMessage) {
                    val avroMsg = AvroConverter.toAvroForMonitoringPerNameMessage(msg)
                    val msg2 = AvroConverter.fromAvroForMonitoringPerNameMessage(avroMsg)
                    val avroMsg2 = AvroConverter.toAvroForMonitoringPerNameMessage(msg2)
                    msg shouldBe msg2
                    avroMsg shouldBe avroMsg2
                }
                if (msg is ForMonitoringGlobalMessage) {
                    val avroMsg = AvroConverter.toAvroForMonitoringGlobalMessage(msg)
                    val msg2 = AvroConverter.fromAvroForMonitoringGlobalMessage(avroMsg)
                    val avroMsg2 = AvroConverter.toAvroForMonitoringGlobalMessage(msg2)
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
