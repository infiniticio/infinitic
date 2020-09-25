package io.infinitic.common.taskManager.avro

import io.infinitic.common.taskManager.messages.ForTaskEngineMessage
import io.infinitic.common.taskManager.messages.ForMonitoringGlobalMessage
import io.infinitic.common.taskManager.messages.ForMonitoringPerNameMessage
import io.infinitic.common.taskManager.messages.ForWorkerMessage
import io.infinitic.common.taskManager.states.TaskEngineState
import io.infinitic.common.taskManager.states.MonitoringGlobalState
import io.infinitic.common.taskManager.states.MonitoringPerNameState
import io.infinitic.common.taskManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe

class AvroDataTests : StringSpec({

    "TaskEngineState should be avro-reversible" {
        // given
        val state = TestFactory.random(TaskEngineState::class)
        // when
        val avroState = AvroConverter.toStorage(state)
        val state2 = AvroConverter.fromStorage(avroState)
        val avroState2 = AvroConverter.toStorage(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "MonitoringPerNameState should be avro-reversible" {
        // given
        val state = TestFactory.random(MonitoringPerNameState::class)
        // when
        val avroState = AvroConverter.toStorage(state)
        val state2 = AvroConverter.fromStorage(avroState)
        val avroState2 = AvroConverter.toStorage(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    "MonitoringGlobalState should be avro-reversible" {
        // given
        val state = TestFactory.random(MonitoringGlobalState::class)
        // when
        val avroState = AvroConverter.toStorage(state)
        val state2 = AvroConverter.fromStorage(avroState)
        val avroState2 = AvroConverter.toStorage(state2)
        // then
        state2 shouldBe state
        avroState2 shouldBe avroState
    }

    ForTaskEngineMessage::class.sealedSubclasses.forEach {
        include(messagesToTaskEngineShouldBeAvroReversible(TestFactory.random(it)))
    }

    ForMonitoringPerNameMessage::class.sealedSubclasses.forEach {
        include(messagesToMonitoringPerNameShouldBeAvroReversible(TestFactory.random(it)))
    }

    ForMonitoringGlobalMessage::class.sealedSubclasses.forEach {
        include(messagesToMonitoringGlobalShouldBeAvroReversible(TestFactory.random(it)))
    }

    ForWorkerMessage::class.sealedSubclasses.forEach {
        include(messagesToWorkersShouldBeAvroReversible(TestFactory.random(it)))
    }
})

internal fun stateShouldBeAvroReversible(msg: ForWorkerMessage) = stringSpec {
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

internal fun messagesToWorkersShouldBeAvroReversible(msg: ForWorkerMessage) = stringSpec {
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

internal fun messagesToTaskEngineShouldBeAvroReversible(msg: ForTaskEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} should be avro-convertible" {
        shouldNotThrowAny {
            val avroMsg = AvroConverter.toTaskEngine(msg)
            val msg2 = AvroConverter.fromTaskEngine(avroMsg)
            val avroMsg2 = AvroConverter.toTaskEngine(msg2)
            msg shouldBe msg2
            avroMsg shouldBe avroMsg2
        }
    }
}

internal fun messagesToMonitoringPerNameShouldBeAvroReversible(msg: ForMonitoringPerNameMessage) = stringSpec {
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

internal fun messagesToMonitoringGlobalShouldBeAvroReversible(msg: ForMonitoringGlobalMessage) = stringSpec {
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
