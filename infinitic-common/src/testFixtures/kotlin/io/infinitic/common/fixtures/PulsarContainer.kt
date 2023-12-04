/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.fixtures

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.utility.DockerImageName

// see https://github.com/testcontainers/testcontainers-java/blob/main/modules/pulsar/src/main/java/org/testcontainers/containers/PulsarContainer.java
class PulsarContainer(dockerImageName: DockerImageName) :
  GenericContainer<PulsarContainer?>(dockerImageName) {
  private val waitAllStrategy = WaitAllStrategy()
  private var functionsWorkerEnabled = false
  private var transactionsEnabled = false

  init {
    dockerImageName.assertCompatibleWith(DockerImageName.parse("apachepulsar/pulsar"))
    withExposedPorts(BROKER_PORT, BROKER_HTTP_PORT)
    setWaitStrategy(waitAllStrategy)
  }

  override fun configure() {
    super.configure()
    setupCommandAndEnv()
  }

  @Suppress("unused")
  fun withFunctionsWorker(): PulsarContainer {
    functionsWorkerEnabled = true
    return this
  }

  @Suppress("unused")
  fun withTransactions(): PulsarContainer {
    transactionsEnabled = true
    return this
  }

  val pulsarBrokerUrl: String
    get() = java.lang.String.format("pulsar://%s:%s", host, getMappedPort(BROKER_PORT))
  val httpServiceUrl: String
    get() = java.lang.String.format("http://%s:%s", host, getMappedPort(BROKER_HTTP_PORT))

  protected fun setupCommandAndEnv() {
    var standaloneBaseCommand =
        "/pulsar/bin/apply-config-from-env.py /pulsar/conf/standalone.conf " + "&& bin/pulsar standalone"
    if (!functionsWorkerEnabled) {
      standaloneBaseCommand += " --no-functions-worker -nss"
    }
    withCommand("/bin/bash", "-c", standaloneBaseCommand)
    val clusterName: String = envMap.getOrDefault("PULSAR_PREFIX_clusterName", "standalone")
    val response = String.format("[\"%s\"]", clusterName)
    waitAllStrategy.withStrategy(
        Wait.forHttp(ADMIN_CLUSTERS_ENDPOINT).forPort(BROKER_HTTP_PORT)
            .forResponsePredicate { anObject: String? ->
              response.equals(
                  anObject,
              )
            },
    )
    if (transactionsEnabled) {
      withEnv("PULSAR_PREFIX_transactionCoordinatorEnabled", "true")
      waitAllStrategy.withStrategy(
          Wait.forHttp(TRANSACTION_TOPIC_ENDPOINT).forStatusCode(200).forPort(BROKER_HTTP_PORT),
      )
    }
    if (functionsWorkerEnabled) {
      waitAllStrategy.withStrategy(Wait.forLogMessage(".*Function worker service started.*", 1))
    }
  }

  companion object {
    const val BROKER_PORT = 6650
    const val BROKER_HTTP_PORT = 8080

    private const val ADMIN_CLUSTERS_ENDPOINT = "/admin/v2/clusters"

    /**
     * See [SystemTopicNames](https://github.com/apache/pulsar/blob/master/pulsar-common/src/main/java/org/apache/pulsar/common/naming/SystemTopicNames.java).
     */
    private const val TRANSACTION_TOPIC_ENDPOINT =
        "/admin/v2/persistent/pulsar/system/transaction_coordinator_assign/partitions"
  }
}
