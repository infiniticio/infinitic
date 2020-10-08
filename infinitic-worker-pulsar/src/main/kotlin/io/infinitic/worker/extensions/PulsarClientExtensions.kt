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

package io.infinitic.worker.extensions

import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.messaging.pulsar.Topic
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType

fun PulsarClient.newTaskConsumer(name: String): Consumer<AvroEnvelopeForWorker> =
    newConsumer(Schema.AVRO(AvroEnvelopeForWorker::class.java))
        .topic(Topic.WORKERS.get(name)) // FIXME: Should probably not be access an internal detail of the pulsar messaging
        .subscriptionName("infinitic-worker") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Shared)
        .subscribe()
