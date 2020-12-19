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
