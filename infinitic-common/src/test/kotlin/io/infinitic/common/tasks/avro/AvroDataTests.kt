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

package io.infinitic.common.tasks.avro

import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.tasks.states.TaskEngineState
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.common.tasks.utils.TestFactory
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
