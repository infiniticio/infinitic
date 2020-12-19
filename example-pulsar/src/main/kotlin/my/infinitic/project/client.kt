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

package my.infinitic.project

import com.sksamuel.hoplite.ConfigLoader
import io.infinitic.client.Client
import io.infinitic.pulsar.config.Config
import io.infinitic.pulsar.transport.PulsarOutputs
import kotlinx.coroutines.runBlocking
import my.infinitic.project.workflows.Add10AndMultiplyBy2
import org.apache.pulsar.client.api.PulsarClient

fun main() {
    val config: Config = ConfigLoader().loadConfigOrThrow("/infinitic.yml")

    val pulsarClient = PulsarClient.builder().serviceUrl(config.pulsar.serviceUrl).build()

    runBlocking {
        val client = Client(
            PulsarOutputs.from(pulsarClient, config.pulsar.tenant, config.pulsar.namespace).clientOutput
        )

        repeat(10) {
            client.dispatch(Add10AndMultiplyBy2::class.java) { handle(10) }
        }
    }
}
