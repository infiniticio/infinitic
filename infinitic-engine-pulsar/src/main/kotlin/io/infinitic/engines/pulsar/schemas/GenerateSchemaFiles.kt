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

package io.infinitic.engines.pulsar.schemas

import com.sksamuel.avro4k.Avro
import io.infinitic.common.json.Json
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.avro.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalEnvelope
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineEnvelope
import io.infinitic.common.tasks.messages.workerMessages.WorkerEnvelope
import java.io.File
import org.apache.pulsar.client.impl.schema.AvroSchema

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

    listOf(
        AvroEnvelopeForTaskEngine::class,
        AvroEnvelopeForWorker::class,
        AvroEnvelopeForMonitoringPerName::class,
        AvroEnvelopeForMonitoringGlobal::class,
        AvroEnvelopeForWorkflowEngine::class
    ).map { klass ->
        File(System.getProperty("user.dir") + "/build/schemas/${klass.simpleName}.schema")
            .also { it.parentFile.mkdirs() }
            .writeText(Json.stringify(PulsarSchema(AvroSchema.of(klass.java).avroSchema.toString())))
    }

//    File(System.getProperty("user.dir") + "/build/schemas/WorkflowEngine.schema")
//        .also { it.parentFile.mkdirs() }
//        .writeText(Json.stringify(PulsarSchema(Avro.default.schema(WorkflowEngineEnvelope.serializer()).toString())))

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
