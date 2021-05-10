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

package io.infinitic.pulsar.workers

import io.infinitic.client.AbstractInfiniticClient
import io.infinitic.client.worker.startClientWorker
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer

typealias PulsarClientMessageToProcess = PulsarMessageToProcess<ClientMessage>

fun CoroutineScope.startClientResponseWorker(
    client: AbstractInfiniticClient,
    consumer: Consumer<ClientEnvelope>
) = launch {

    val inputChannel = Channel<PulsarClientMessageToProcess>()
    val outputChannel = Channel<PulsarClientMessageToProcess>()

    // launch n=concurrency coroutines running task executors
    startClientWorker(
        client,
        inputChannel,
        outputChannel
    )

    // coroutine pulling pulsar commands messages
    pullMessages(consumer, inputChannel)

    // coroutine acknowledging pulsar commands messages
    acknowledgeMessages(consumer, outputChannel)
}
