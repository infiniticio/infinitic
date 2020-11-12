package io.infinitic.common.tasks

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.workers.messages.WorkerEnvelope
import io.infinitic.common.workers.messages.WorkerMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnvelopesTests : StringSpec({

    TaskEngineMessage::class.sealedSubclasses.map {
        val msg = TestFactory.random(it)

        "TaskEngineEnvelope(${msg::class.simpleName}) should be avro-convertible" {
            shouldNotThrowAny {
                val envelope = TaskEngineEnvelope.from(msg)
                val ser = TaskEngineEnvelope.serializer()
                val byteArray = Avro.default.encodeToByteArray(ser, envelope)
                val envelope2 = Avro.default.decodeFromByteArray(ser, byteArray)
                envelope shouldBe envelope2
            }
        }
    }

    MonitoringPerNameEngineMessage::class.sealedSubclasses.map {
        val msg = TestFactory.random(it)

        "MonitoringPerNameEnvelope(${msg::class.simpleName}) should be avro-convertible" {
            shouldNotThrowAny {
                val envelope = MonitoringPerNameEnvelope.from(msg)
                val ser = MonitoringPerNameEnvelope.serializer()
                val byteArray = Avro.default.encodeToByteArray(ser, envelope)
                val envelope2 = Avro.default.decodeFromByteArray(ser, byteArray)
                envelope shouldBe envelope2
            }
        }
    }

    MonitoringGlobalMessage::class.sealedSubclasses.map {
        val msg = TestFactory.random(it)

        "MonitoringGlobalEnvelope(${msg::class.simpleName}) should be avro-convertible" {
            shouldNotThrowAny {
                val envelope = MonitoringGlobalEnvelope.from(msg)
                val ser = MonitoringGlobalEnvelope.serializer()
                val byteArray = Avro.default.encodeToByteArray(ser, envelope)
                val envelope2 = Avro.default.decodeFromByteArray(ser, byteArray)
                envelope shouldBe envelope2
            }
        }
    }

    WorkerMessage::class.sealedSubclasses.map {
        val msg = TestFactory.random(it)

        "WorkerEnvelope(${msg::class.simpleName}) should be avro-convertible" {
            shouldNotThrowAny {
                val envelope = WorkerEnvelope.from(msg)
                val ser = WorkerEnvelope.serializer()
                val byteArray = Avro.default.encodeToByteArray(ser, envelope)
                val envelope2 = Avro.default.decodeFromByteArray(ser, byteArray)
                envelope shouldBe envelope2
            }
        }
    }
})
