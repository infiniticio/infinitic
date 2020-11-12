package io.infinitic.common.tasks

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.monitoringGlobal.state.MonitoringGlobalState
import io.infinitic.common.monitoringPerName.state.MonitoringPerNameState
import io.infinitic.common.tasks.state.TaskState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StatesTests : StringSpec({

    "TaskState should be avro-convertible" {
        shouldNotThrowAny {
            val state = TestFactory.random<TaskState>()
            state shouldBe TaskState.fromByteArray(state.toByteArray())
        }
    }

    "MonitoringPerNameState should be avro-convertible" {
        shouldNotThrowAny {
            val state = TestFactory.random<MonitoringPerNameState>()
            state shouldBe MonitoringPerNameState.fromByteArray(state.toByteArray())
        }
    }

    "MonitoringGlobalState should be avro-convertible" {
        shouldNotThrowAny {
            val state = TestFactory.random<MonitoringGlobalState>()
            state shouldBe MonitoringGlobalState.fromByteArray(state.toByteArray())
        }
    }
})
