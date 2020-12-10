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

package io.infinitic.pulsar.schemas

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.json.Json
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.serDe.kotlin.kserializer
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import org.apache.pulsar.common.protocol.schema.PostSchemaPayload
import java.io.File
import kotlin.reflect.KClass

/**
 * Creates pulsar schema files in /build/schemas
 */
fun main() {
    println(getPostSchemaPayload(WorkflowEngineEnvelope::class))

    // make sure target directory exists
    val path = System.getProperty("user.dir") + "/build/schemas"
    File(path).mkdir()

    File("$path/WorkflowEngine.schema")
        .writeText(Json.stringify(getPostSchemaPayload(WorkflowEngineEnvelope::class)))

    File("$path/TaskEngine.schema")
        .writeText(Json.stringify(getPostSchemaPayload(TaskEngineEnvelope::class)))

    File("$path/MonitoringPerName.schema")
        .writeText(Json.stringify(getPostSchemaPayload(MonitoringPerNameEnvelope::class)))

    File("$path/MonitoringGlobal.schema")
        .writeText(Json.stringify(getPostSchemaPayload(MonitoringGlobalEnvelope::class)))

    File("$path/TaskExecutor.schema")
        .writeText(Json.stringify(getPostSchemaPayload(TaskExecutorEnvelope::class)))
}

fun <T : Any> getPostSchemaPayload(klass: KClass<T>) = PostSchemaPayload(
    "AVRO",
    Avro.default.schema(kserializer(klass)).toString(),
    mapOf()
)
