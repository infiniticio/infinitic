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

package io.infinitic.pulsar.messageBuilders

import io.infinitic.pulsar.schemas.schemaDefinition
import kotlinx.coroutines.future.await
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import java.util.concurrent.TimeUnit

interface PulsarMessageBuilder {
    fun <O> newMessage(topicName: String, schema: Schema<O>): TypedMessageBuilder<O>
}

suspend inline fun <reified T : Any> PulsarMessageBuilder.sendPulsarMessage(topic: String, msg: T, key: String?, after: Float) {
    this
        .newMessage(
            topic,
            AvroSchema.of(schemaDefinition<T>())
        )
        .value(msg)
        .also {
            if (key != null) {
                it.key(key)
            }
            if (after > 0F) {
                it.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
            }
        }
        .sendAsync()
        .await()
}
