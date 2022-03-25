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

package io.infinitic.common.workflows.engine.state

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.fixtures.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.apache.avro.Schema
import org.apache.avro.SchemaCompatibility

class WorkflowStateTests : StringSpec({
    "WorkflowState should be avro-convertible" {
        shouldNotThrowAny {
            val msg = TestFactory.random<WorkflowState>()
            val ser = WorkflowState.serializer()
            val byteArray = Avro.default.encodeToByteArray(ser, msg)
            val msg2 = Avro.default.decodeFromByteArray(ser, byteArray)
            msg shouldBe msg2
        }
    }

    "WorkflowState should be backward-compatible with v0.9.0" {
        val newSchema = Avro.default.schema(WorkflowState.serializer())

        val fileContent = WorkflowStateTests::class.java.classLoader
            .getResource("workflowState-0.9.0.avsc")!!.readText()
        val oldSchema = Schema.Parser().parse(fileContent)

        SchemaCompatibility.checkReaderWriterCompatibility(newSchema, oldSchema)
            .type shouldBe SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE
    }
})
