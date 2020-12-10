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

package io.infinitic.pulsar.transport

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameOutput
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context

class PulsarMonitoringPerNameOutput(private val pulsarMessageBuilder: PulsarMessageBuilder) : MonitoringPerNameOutput {
    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(client: PulsarClient) = PulsarMonitoringPerNameOutput(PulsarMessageBuilderFromClient(client))

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarMonitoringPerNameOutput(PulsarMessageBuilderFromFunction(context))
    }

    override val sendToMonitoringGlobal: SendToMonitoringGlobal = { message: MonitoringGlobalMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            MonitoringGlobalTopic.name,
            MonitoringGlobalEnvelope.from(message),
            null,
            0F
        )
    }
}
