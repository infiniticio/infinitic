package io.infinitic.common.tasks

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.common.tasks.states.TaskEngineState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StatesTests : StringSpec({

    "TaskEngineState should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<TaskEngineState>()
            val ser = TaskEngineState.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }

    "MonitoringPerNameState should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<MonitoringPerNameState>()
            val ser = MonitoringPerNameState.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }

    "MonitoringGlobalState should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<MonitoringGlobalState>()
            val ser = MonitoringGlobalState.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }
})
