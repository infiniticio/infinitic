/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.tasks

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.metrics.global.state.MetricsGlobalState
import io.infinitic.common.metrics.perName.state.MetricsPerNameState
import io.infinitic.common.tasks.engine.state.TaskState
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
            val state = TestFactory.random<MetricsPerNameState>()
            state shouldBe MetricsPerNameState.fromByteArray(state.toByteArray())
        }
    }

    "MonitoringGlobalState should be avro-convertible" {
        shouldNotThrowAny {
            val state = TestFactory.random<MetricsGlobalState>()
            state shouldBe MetricsGlobalState.fromByteArray(state.toByteArray())
        }
    }
})
