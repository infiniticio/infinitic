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

import io.infinitic.pulsar.admin.initInfinitic
import org.apache.pulsar.client.admin.PulsarAdmin

fun main() {
    val url = "http://localhost:8080"
    // Pass auth-plugin class fully-qualified name if Pulsar-security enabled
    val authPluginClassName = "com.org.MyAuthPluginClass"
    // Pass auth-param if auth-plugin class requires it
    val authParams = "param1=value1"
    val useTls = false
    val tlsAllowInsecureConnection = false
    val tlsTrustCertsFilePath = null
    val admin = PulsarAdmin.builder()
//        .authentication(authPluginClassName,authParams)
        .serviceHttpUrl(url)
        .tlsTrustCertsFilePath(tlsTrustCertsFilePath)
        .allowTlsInsecureConnection(tlsAllowInsecureConnection)
        .build()

    admin.initInfinitic("infinitic", "dev")
//    val schema = admin.schemas().getSchemaInfo("persistent://public/default/workflows-engine")

//    println(schema)

//    println(admin.schemas().createSchema(
//        "persistent://public/default/workflows-engine",
//        getPostSchemaPayload(WorkflowEngineEnvelope::class)
//    ))

//    val pulsarClient = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build()
//    val transport = PulsarTransport.from(pulsarClient)
//
//    val client = Client(
//        transport.sendToTaskEngineCommands,
//        transport.sendToWorkflowEngineCommands
//    )
//
//    val consumer =  pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<WorkflowEngineEnvelope>()))
//        .topic(Topic.WORKFLOW_ENGINE.get())
//        .subscriptionName("infinitic-workflow-engine")
//        .subscriptionType(SubscriptionType.Key_Shared)
//        .subscribe()
//
//    runBlocking {
//
//        launch {
//            while (isActive) {
//                val message: Message<WorkflowEngineEnvelope> = consumer.receiveAsync().await()
//
//                try {
//                    println(readBinary(message.data, WorkflowEngineEnvelope.serializer()))
//                    consumer.acknowledgeAsync(message).await()
//                } catch (e: Exception) {
//                    consumer.negativeAcknowledge(message)
//                    throw e
//                }
//            }
//        }
//
//        client.dispatch(WorkflowA::class.java) { seq1() }
//        client.dispatch(WorkflowA::class.java) { seq1() }
//        client.dispatch(WorkflowA::class.java) { seq1() }
//    }

//    val msg = WorkflowEngineEnvelope.from(TestFactory.random<DispatchWorkflow>())
//    println(msg)
//
//    val schema: Schema<WorkflowEngineEnvelope> = Schema.AVRO(
//        SchemaDefinition.builder<WorkflowEngineEnvelope>()
//            .withAlwaysAllowNull(true)
//            .withJSR310ConversionEnabled(true)
//            .withJsonDef(Avro.default.schema(kserializer(WorkflowEngineEnvelope::class)).toString())
//            .withSchemaReader(KSchemaReader(WorkflowEngineEnvelope::class))
//            .withSchemaWriter(KSchemaWriter(WorkflowEngineEnvelope::class))
//            .withSupportSchemaVersioning(true)
//            .build()
//    )
//
//    val producer: Producer<WorkflowEngineEnvelope> = pulsarClient
//        .newProducer(schema)
//        .topic("persistent://public/default/workflows-engine")
//        .create()
//
//    schema.encode(msg)
//
//    producer.newMessage().value(msg).send()

    println("done")
}
