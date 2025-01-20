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
package io.infinitic.transport.config

import io.infinitic.properties.isLazyInitialized
import io.infinitic.pulsar.PulsarInfiniticConsumerFactory
import io.infinitic.pulsar.PulsarInfiniticProducerFactory
import io.infinitic.pulsar.PulsarInfiniticResources
import io.infinitic.pulsar.config.PulsarClientConfig
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.PulsarConsumerConfig
import io.infinitic.pulsar.config.PulsarProducerConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import java.net.URLEncoder

@Suppress("unused")
data class PulsarTransportConfig(
  val pulsar: PulsarConfig,
  override val shutdownGracePeriodSeconds: Double = 30.0
) : TransportConfig() {

  init {
    require(shutdownGracePeriodSeconds > 0) { "shutdownGracePeriodSeconds must be > 0" }
  }

  override fun close() {
    if (pulsar::pulsarAdmin.isLazyInitialized) pulsar.pulsarAdmin.close()
    if (pulsar::pulsarClient.isLazyInitialized) pulsar.pulsarClient.close()
  }

  /** This is used as source prefix for CloudEvents */
  override val cloudEventSourcePrefix: String = pulsar.brokerServiceUrl.removeSuffix("/") + "/" +
      URLEncoder.encode(pulsar.tenant, Charsets.UTF_8) + "/" +
      URLEncoder.encode(pulsar.namespace, Charsets.UTF_8)


  /** Infinitic Resources */
  override val resources by lazy { PulsarInfiniticResources(pulsar.pulsarResources) }

  /** Infinitic Consumer */
  override val consumerFactory by lazy {
    PulsarInfiniticConsumerFactory(
        pulsar.infiniticPulsarClient,
        pulsar.consumer,
        pulsar.pulsarResources,
    )
  }

  /** Infinitic Producer */
  override val producerFactory by lazy {
    PulsarInfiniticProducerFactory(
        pulsar.infiniticPulsarClient,
        pulsar.producer,
        pulsar.pulsarResources,
    )
  }

  companion object {
    @JvmStatic
    fun builder() = PulsarTransportConfigBuilder()
  }

  /**
   * PulsarConfig builder
   */
  class PulsarTransportConfigBuilder : TransportConfigBuilder {
    private var shutdownGracePeriodSeconds: Double = 30.0
    private var brokerServiceUrl: String? = null
    private var webServiceUrl: String? = null
    private var tenant: String? = null
    private var namespace: String? = null
    private var allowedClusters: Set<String>? = null
    private var adminRoles: Set<String>? = null
    private var client: PulsarClientConfig = PulsarClientConfig()
    private var policies: PoliciesConfig = PoliciesConfig()
    private var producer: PulsarProducerConfig = PulsarProducerConfig()
    private var consumer: PulsarConsumerConfig = PulsarConsumerConfig()

    fun setShutdownGracePeriodSeconds(shutdownGracePeriodSeconds: Double) =
        apply { this.shutdownGracePeriodSeconds = shutdownGracePeriodSeconds }

    fun setTenant(tenant: String) =
        apply { this.tenant = tenant }

    fun setNamespace(namespace: String) =
        apply { this.namespace = namespace }

    fun setBrokerServiceUrl(brokerServiceUrl: String) =
        apply { this.brokerServiceUrl = brokerServiceUrl }

    fun setWebServiceUrl(webServiceUrl: String) =
        apply { this.webServiceUrl = webServiceUrl }

    fun setAllowedClusters(allowedClusters: Set<String>) =
        apply { this.allowedClusters = allowedClusters }

    fun setAdminRoles(adminRoles: Set<String>) =
        apply { this.adminRoles = adminRoles }

    fun setClientConfig(pulsarClientConfig: PulsarClientConfig) =
        apply { this.client = pulsarClientConfig }

    fun setClientConfig(pulsarClientConfig: PulsarClientConfig.ClientConfigBuilder) =
        setClientConfig(pulsarClientConfig.build())

    fun setPolicies(policiesConfig: PoliciesConfig) =
        apply { this.policies = policiesConfig }

    fun setPolicies(policiesConfig: PoliciesConfig.PoliciesConfigBuilder) =
        setPolicies(policiesConfig.build())

    fun setProducerConfig(pulsarProducerConfig: PulsarProducerConfig) =
        apply { this.producer = pulsarProducerConfig }

    fun setProducerConfig(pulsarProducerConfig: PulsarProducerConfig.ProducerConfigBuilder) =
        setProducerConfig(pulsarProducerConfig.build())

    fun setConsumerConfig(pulsarConsumerConfig: PulsarConsumerConfig) =
        apply { this.consumer = pulsarConsumerConfig }

    fun setConsumerConfig(pulsarConsumerConfig: PulsarConsumerConfig.ConsumerConfigBuilder) =
        setConsumerConfig(pulsarConsumerConfig.build())

    override fun build(): PulsarTransportConfig {
      require(brokerServiceUrl != null) { "${PulsarConfig::brokerServiceUrl.name} must not be null" }
      require(webServiceUrl != null) { "${PulsarConfig::webServiceUrl.name} must not be null" }
      require(tenant != null) { "${PulsarConfig::tenant.name} must not be null" }
      require(namespace != null) { "${PulsarConfig::namespace.name} must not be null" }

      val pulsarConfig = PulsarConfig(
          brokerServiceUrl!!,
          webServiceUrl!!,
          tenant!!,
          namespace!!,
          allowedClusters,
          adminRoles,
          client,
          policies,
          producer,
          consumer,
      )

      return PulsarTransportConfig(
          pulsar = pulsarConfig,
          shutdownGracePeriodSeconds = shutdownGracePeriodSeconds,
      )
    }
  }
}

