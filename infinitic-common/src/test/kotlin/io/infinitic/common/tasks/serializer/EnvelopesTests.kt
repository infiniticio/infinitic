package io.infinitic.common.tasks.serializer

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalEnvelope
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineEnvelope
import io.infinitic.common.tasks.messages.workerMessages.WorkerEnvelope
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnvelopesTests : StringSpec({

    "TaskEngineEnvelope should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<TaskEngineEnvelope>()
            val ser =  TaskEngineEnvelope.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }

    "MonitoringPerNameEnvelope should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<MonitoringPerNameEnvelope>()
            val ser =  MonitoringPerNameEnvelope.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }

    "MonitoringGlobalEnvelope should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<MonitoringGlobalEnvelope>()
            val ser =  MonitoringGlobalEnvelope.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }

    "WorkerEnvelope should be avro-convertible" {
            shouldNotThrowAny {
                val msg = TestFactory.random<WorkerEnvelope>()
                val ser =  WorkerEnvelope.serializer()
                val byteArray = Avro.default.encodeToByteArray(ser, msg)
                val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
                msg shouldBe msg2
            }
        }
})
