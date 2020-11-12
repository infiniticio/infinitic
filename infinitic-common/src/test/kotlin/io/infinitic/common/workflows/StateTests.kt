package io.infinitic.common.workflows

import com.sksamuel.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.state.WorkflowState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StateTests : StringSpec({
    "WorkflowState should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<WorkflowState>()
            val ser = WorkflowState.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }
})
