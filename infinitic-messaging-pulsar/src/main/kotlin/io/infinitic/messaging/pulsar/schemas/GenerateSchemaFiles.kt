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

package io.infinitic.messaging.pulsar.schemas

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.json.Json
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.common.workers.messages.WorkerEnvelope
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import java.io.File

/**
 * This class creates files in /build/schemas, used to upload schemas to topics
 */
fun main() {

    class PulsarSchema(schema: String) {
        val type = "AVRO"
        val properties = mapOf(
            "__alwaysAllowNull" to "true",
            "__jsr310ConversionEnabled" to "false"
        )
        val schema = schema
    }

    File(System.getProperty("user.dir") + "/build/schemas/WorkflowEngine.schema")
        .also { it.parentFile.mkdirs() }
        .writeText(Json.stringify(PulsarSchema(Avro.default.schema(WorkflowEngineEnvelope.serializer()).toString())))

    File(System.getProperty("user.dir") + "/build/schemas/TaskEngine.schema")
        .also { it.parentFile.mkdirs() }
        .writeText(Json.stringify(PulsarSchema(Avro.default.schema(TaskEngineEnvelope.serializer()).toString())))

    File(System.getProperty("user.dir") + "/build/schemas/MonitoringPerName.schema")
        .also { it.parentFile.mkdirs() }
        .writeText(Json.stringify(PulsarSchema(Avro.default.schema(MonitoringPerNameEnvelope.serializer()).toString())))

    File(System.getProperty("user.dir") + "/build/schemas/MonitoringGlobal.schema")
        .also { it.parentFile.mkdirs() }
        .writeText(Json.stringify(PulsarSchema(Avro.default.schema(MonitoringGlobalEnvelope.serializer()).toString())))

    File(System.getProperty("user.dir") + "/build/schemas/Worker.schema")
        .also { it.parentFile.mkdirs() }
        .writeText(Json.stringify(PulsarSchema(Avro.default.schema(WorkerEnvelope.serializer()).toString())))
}
