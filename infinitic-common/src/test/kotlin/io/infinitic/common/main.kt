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

package io.infinitic.common

import Ci
import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.state.WorkflowStateTests
import org.apache.avro.Schema
import java.io.File

/**
 * This creates the current schema of the workflow state
 */
fun main() {
    val file = WorkflowStateTests::class.java.classLoader.getResource("workflowState.avsc")!!.file.replace("/build/resources/test/", "/src/test/resources/")
    val versionedFile = file.split(".").let { it[0] + "-${Ci.version}." + it[1] }

    val schema: Schema = Avro.default.schema(WorkflowState.serializer())
    File(versionedFile).writeText(schema.toString(true))
}
