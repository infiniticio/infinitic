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

package io.infinitic.pulsar

import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.samples.TaskA
import io.infinitic.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.client.impl.schema.util.SchemaUtil
import org.apache.pulsar.common.schema.SchemaType

// val test: (Int) -> Int = { a:Int -> 2*a }
// pow = lambda  a: a*a
// pow([2, 4, 3], 2)

// list(map(pow, [2, 4, 3]))
// val w = test(test(3))

fun main() {
    val s = schemaDefinition<WorkflowEngineEnvelope>()
    val sc = SchemaUtil.parseSchemaInfo(s, SchemaType.AVRO)

//    AvroSchema.addLogicalTypeConversions(ReflectData(), true)

    val d = AvroSchema.of(TaskA::class.java)

//    val url = "http://localhost:8080"
//    // Pass auth-plugin class fully-qualified name if Pulsar-security enabled
//    val authPluginClassName = "com.org.MyAuthPluginClass"
//    // Pass auth-param if auth-plugin class requires it
//    val authParams = "param1=value1"
//    val useTls = false
//    val tlsAllowInsecureConnection = false
//    val tlsTrustCertsFilePath = null
//    val admin = PulsarAdmin.builder()
//        //        .authentication(authPluginClassName,authParams)
//        .serviceHttpUrl(url)
//        .tlsTrustCertsFilePath(tlsTrustCertsFilePath)
//        .allowTlsInsecureConnection(tlsAllowInsecureConnection)
//        .build()
//
//    runBlocking {
//        admin.infiniticInit("infinitic", "dev5")
//    }
}
