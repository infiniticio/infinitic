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

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.common.tasks.utils.TestFactory
import io.infinitic.avro.taskManager.data.states.AvroTaskEngineState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AvroSerDeTests : StringSpec({
    "Avro state should be SerDe-reversible through ByteBuffer" {
        // given
        val state = TestFactory.random(AvroTaskEngineState::class)
        // when
        val serState = AvroSerDe.serialize(state)
        val deState = AvroSerDe.deserialize<AvroTaskEngineState>(serState)
        // then
        state shouldBe deState
    }

    "Avro state should be SerDe-reversible through ByteArray" {
        // given
        val state = TestFactory.random(AvroTaskEngineState::class)
        // when
        val serState = AvroSerDe.serializeToByteArray(state)
        val deState = AvroSerDe.deserializeFromByteArray<AvroTaskEngineState>(serState)
        // then
        state shouldBe deState
    }
})
