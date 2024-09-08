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

import com.sksamuel.hoplite.Secret
import io.infinitic.autoclose.addAutoCloseResource
import io.infinitic.pulsar.PulsarInfiniticConsumer
import io.infinitic.pulsar.PulsarInfiniticProducer
import io.infinitic.pulsar.PulsarInfiniticResources
import io.infinitic.pulsar.client.PulsarInfiniticClient
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.TlsTrustStoreType
import io.infinitic.pulsar.config.auth.ClientAuthenticationConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.consumers.ConsumerConfig
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.producers.ProducerConfig
import io.infinitic.pulsar.resources.PulsarResources
import java.net.URLEncoder

data class PulsarTransportConfig(
  val pulsar: PulsarConfig,
  override val shutdownGracePeriodSeconds: Double = 30.0
) : TransportConfig() {

  init {
    require(shutdownGracePeriodSeconds >= 0) { "shutdownGracePeriodSeconds must be >= 0" }
  }

  companion object {
    @JvmStatic
    fun builder() = PulsarTransportConfigBuilder()
  }

  /** This is used as source prefix for CloudEvents */
  override val cloudEventSource: String = pulsar.brokerServiceUrl.removeSuffix("/") + "/" +
      URLEncoder.encode(pulsar.tenant, Charsets.UTF_8) + "/" +
      URLEncoder.encode(pulsar.namespace, Charsets.UTF_8)


  private val client = PulsarInfiniticClient(pulsar.client)
  private val pulsarResources = PulsarResources.from(pulsar).also {
    it.addAutoCloseResource(pulsar.admin)
  }

  /** Infinitic Resources */
  override val resources = PulsarInfiniticResources(pulsarResources)

  /** Infinitic Consumer */
  override val consumer = PulsarInfiniticConsumer(
      Consumer(client, pulsar.consumer),
      pulsarResources,
      shutdownGracePeriodSeconds,
  ).also {
    it.addAutoCloseResource(pulsar.client)
  }

  /** Infinitic Producer */
  override val producer = PulsarInfiniticProducer(
      Producer(client, pulsar.producer),
      pulsarResources,
  )

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
    private var tlsAllowInsecureConnection: Boolean? = null
    private var tlsEnableHostnameVerification: Boolean? = null
    private var tlsTrustCertsFilePath: String? = null
    private var useKeyStoreTls: Boolean? = null
    private var tlsTrustStoreType: TlsTrustStoreType? = null
    private var tlsTrustStorePath: String? = null
    private var tlsTrustStorePassword: Secret? = null
    private var authentication: ClientAuthenticationConfig? = null
    private var policies: PoliciesConfig = PoliciesConfig()
    private var producer: ProducerConfig = ProducerConfig()
    private var consumer: ConsumerConfig = ConsumerConfig()

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

    fun setTlsAllowInsecureConnection(tlsAllowInsecureConnection: Boolean) =
        apply { this.tlsAllowInsecureConnection = tlsAllowInsecureConnection }

    fun setTlsEnableHostnameVerification(tlsEnableHostnameVerification: Boolean) =
        apply { this.tlsEnableHostnameVerification = tlsEnableHostnameVerification }

    fun setTlsTrustCertsFilePath(tlsTrustCertsFilePath: String) =
        apply { this.tlsTrustCertsFilePath = tlsTrustCertsFilePath }

    fun setUseKeyStoreTls(useKeyStoreTls: Boolean) =
        apply { this.useKeyStoreTls = useKeyStoreTls }

    fun setTlsTrustStoreType(tlsTrustStoreType: TlsTrustStoreType) =
        apply { this.tlsTrustStoreType = tlsTrustStoreType }

    fun setTlsTrustStorePath(tlsTrustStorePath: String) =
        apply { this.tlsTrustStorePath = tlsTrustStorePath }

    fun setTlsTrustStorePassword(tlsTrustStorePassword: Secret) =
        apply { this.tlsTrustStorePassword = tlsTrustStorePassword }

    fun setAuthentication(authentication: ClientAuthenticationConfig) =
        apply { this.authentication = authentication }

    fun setPolicies(policiesConfig: PoliciesConfig) =
        apply { this.policies = policiesConfig }

    fun setPolicies(policiesConfig: PoliciesConfig.PoliciesConfigBuilder) =
        setPolicies(policiesConfig.build())

    fun setProducer(producerConfig: ProducerConfig) =
        apply { this.producer = producerConfig }

    fun setProducer(producerConfig: ProducerConfig.ProducerConfigBuilder) =
        setProducer(producerConfig.build())

    fun setConsumer(consumerConfig: ConsumerConfig) =
        apply { this.consumer = consumerConfig }

    fun setConsumer(consumerConfig: ConsumerConfig.ConsumerConfigBuilder) =
        setConsumer(consumerConfig.build())

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
          tlsAllowInsecureConnection,
          tlsEnableHostnameVerification,
          tlsTrustCertsFilePath,
          useKeyStoreTls,
          tlsTrustStoreType,
          tlsTrustStorePath,
          tlsTrustStorePassword,
          authentication,
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

