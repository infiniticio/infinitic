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
package io.infinitic.pulsar.config

import com.sksamuel.hoplite.Secret
import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.pulsar.config.auth.AuthenticationAthenzConfig
import io.infinitic.pulsar.config.auth.AuthenticationOAuth2Config
import io.infinitic.pulsar.config.auth.AuthenticationSaslConfig
import io.infinitic.pulsar.config.auth.AuthenticationTokenConfig
import io.infinitic.pulsar.config.auth.ClientAuthenticationConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.infinitic.pulsar.consumers.ConsumerConfig
import io.infinitic.pulsar.producers.ProducerConfig
import io.infinitic.serDe.java.Json.mapper
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.api.AuthenticationFactory
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.auth.oauth2.AuthenticationFactoryOAuth2
import org.apache.pulsar.client.impl.auth.AuthenticationAthenz as PulsarAuthenticationAthenz
import org.apache.pulsar.client.impl.auth.AuthenticationSasl as PulsarAuthenticationSasl

/**
 * Configuration class for setting up Apache Pulsar connection properties.
 *
 * @property brokerServiceUrl URL for the Pulsar broker service.
 * @property webServiceUrl URL for the Pulsar web service.
 * @property tenant Pulsar tenant name.
 * @property namespace Pulsar namespace name.
 * @property allowedClusters Set of allowed clusters (optional).
 * @property adminRoles Set of admin roles (optional).
 * @property tlsAllowInsecureConnection Whether TLS allows insecure connections (optional).
 * @property tlsEnableHostnameVerification Whether TLS enables hostname verification (optional).
 * @property tlsTrustCertsFilePath Path to the TLS trust certificates file (optional).
 * @property useKeyStoreTls Whether to use KeyStore for TLS (optional).
 * @property tlsTrustStoreType Type of the TLS trust store (e.g., JKS, PKCS12) (optional).
 * @property tlsTrustStorePath Path to the TLS trust store (optional).
 * @property tlsTrustStorePassword Password for the TLS trust store (optional).
 * @property authentication Client authentication configuration (optional).
 * @property policies Pulsar policies configuration (default value provided).
 * @property producer Pulsar producer configuration (default value provided).
 * @property consumer Pulsar consumer configuration (default value provided).
 */
@Suppress("unused")
data class PulsarConfig(
  val brokerServiceUrl: String, // "pulsar://localhost:6650/",
  val webServiceUrl: String, // "http://localhost:8080",
  val tenant: String,
  val namespace: String,
  val allowedClusters: Set<String>? = null,
  val adminRoles: Set<String>? = null,
  val tlsAllowInsecureConnection: Boolean? = null,
  val tlsEnableHostnameVerification: Boolean? = null,
  val tlsTrustCertsFilePath: String? = null,
  val useKeyStoreTls: Boolean? = null,
  val tlsTrustStoreType: TlsTrustStoreType? = null,
  val tlsTrustStorePath: String? = null,
  val tlsTrustStorePassword: Secret? = null,
  val authentication: ClientAuthenticationConfig? = null,
  val policies: PoliciesConfig = PoliciesConfig(),
  val producer: ProducerConfig = ProducerConfig(),
  val consumer: ConsumerConfig = ConsumerConfig()
) {

  companion object {
    @JvmStatic
    fun builder() = PulsarConfigBuilder()

    private val logger = KotlinLogging.logger {}
    private const val PULSAR_PROTOCOL = "pulsar://"
    private const val PULSAR_PROTOCOL_SSL = "pulsar+ssl://"
    private const val HTTP_PROTOCOL = "http://"
    private const val HTTP_PROTOCOL_SSL = "https://"
    private const val HIDDEN = "******"
  }

  init {
    require(
        brokerServiceUrl.startsWith(PULSAR_PROTOCOL) ||
            brokerServiceUrl.startsWith(PULSAR_PROTOCOL_SSL),
    ) {
      when {
        brokerServiceUrl == "" -> "The brokerServiceUrl value MUST be provided. If you're using a " +
            "local Pulsar instance, the value is typically set to pulsar://localhost:6650/"

        else -> "The brokerServiceUrl value MUST start with pulsar:// or pulsar+ssl://"
      }
    }

    require(
        webServiceUrl.startsWith(HTTP_PROTOCOL) ||
            webServiceUrl.startsWith(HTTP_PROTOCOL_SSL),
    ) {
      when {
        webServiceUrl == "" -> "The webServiceUrl value MUST be provided. If you're using a " +
            "local Pulsar instance, the value is typically set to http://localhost:8080"

        else -> "The webServiceUrl value MUST start with http:// or https://"
      }
    }

    require(tenant.isNotEmpty()) { "tenant can NOT be empty" }

    require(namespace.isNotEmpty()) { "namespace can NOT be empty" }

    if (useKeyStoreTls == true) {
      require(tlsTrustStorePath != null) {
        "Configuration Error: 'tlsTrustStorePath' is required when 'useKeyStoreTls' is set to true. Please specify a valid path to the trust store."
      }
      require(tlsTrustStorePassword != null) {
        "Configuration Error: 'tlsTrustStorePassword' is required when 'useKeyStoreTls' is set to true. Please provide the trust store password."
      }
      require(tlsTrustStoreType != null) {
        "Configuration Error: 'tlsTrustStoreType' is required when 'useKeyStoreTls' is set to true. Please specify the type of trust store (e.g., JKS, PKCS12)."
      }
    }
  }

  val admin: PulsarAdmin by lazy {
    val log = mutableMapOf<String, Any?>()
    PulsarAdmin.builder().apply {
      serviceHttpUrl(webServiceUrl)
      log["serviceHttpUrl"] = webServiceUrl

      tlsAllowInsecureConnection?.let {
        allowTlsInsecureConnection(it)
        log["allowTlsInsecureConnection"] = it
      }

      tlsEnableHostnameVerification?.let {
        enableTlsHostnameVerification(it)
        log["enableTlsHostnameVerification"] = it
      }

      tlsTrustCertsFilePath?.let {
        tlsTrustCertsFilePath(it)
        log["tlsTrustCertsFilePath"] = it
      }

      if (useKeyStoreTls == true) {
        useKeyStoreTls(true)
        tlsTrustStoreType(tlsTrustStoreType.toString())
        tlsTrustStorePath(tlsTrustStorePath)
        tlsTrustStorePassword(tlsTrustStorePassword?.value)
        log["useKeyStoreTls"] = true
        log["tlsTrustStoreType"] = tlsTrustStoreType
        log["tlsTrustStorePath"] = tlsTrustStorePath
        log["tlsTrustStorePassword"] = HIDDEN
      }

      when (authentication) {
        is AuthenticationTokenConfig -> {
          authentication(AuthenticationFactory.token(authentication.token.value))
          log["AuthenticationFactory.token"] = HIDDEN
        }

        is AuthenticationAthenzConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationAthenz::class.java.name,
                  mapper.writeValueAsString(authentication),
              ),
          )
          log["AuthenticationFactory.AuthenticationAthenz"] = HIDDEN
        }

        is AuthenticationSaslConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationSasl::class.java.name,
                  mapper.writeValueAsString(authentication),
              ),
          )
          log["AuthenticationFactory.AuthenticationSasl"] = HIDDEN
        }

        is AuthenticationOAuth2Config -> {
          authentication(
              AuthenticationFactoryOAuth2.clientCredentials(
                  authentication.issuerUrl,
                  authentication.privateKey,
                  authentication.audience,
              ),
          )
          log["AuthenticationFactoryOAuth2.issuerUrl"] = authentication.issuerUrl
          log["AuthenticationFactoryOAuth2.privateKey"] = authentication.privateKey
          log["AuthenticationFactoryOAuth2.audience"] = authentication.audience
        }

        null -> Unit
      }
    }.build().also {
      logger.info {
        "Created PulsarAdmin with config: ${log.map { "${it.key}=${it.value}" }.joinToString()}"
      }
    }
  }

  val client: PulsarClient by lazy {
    val log = mutableMapOf<String, Any?>()
    PulsarClient.builder().apply {
      serviceUrl(brokerServiceUrl)
      log["serviceUrl"] = brokerServiceUrl

      tlsAllowInsecureConnection?.let {
        allowTlsInsecureConnection(it)
        log["allowTlsInsecureConnection"] = it
      }

      tlsEnableHostnameVerification?.let {
        enableTlsHostnameVerification(it)
        log["enableTlsHostnameVerification"] = it
      }

      tlsTrustCertsFilePath?.let {
        tlsTrustCertsFilePath(it)
        log["tlsTrustCertsFilePath"] = it
      }

      if (useKeyStoreTls == true) {
        useKeyStoreTls(true)
        tlsTrustStoreType(tlsTrustStoreType.toString())
        tlsTrustStorePath(tlsTrustStorePath)
        tlsTrustStorePassword(tlsTrustStorePassword?.value)
        log["useKeyStoreTls"] = true
        log["tlsTrustStoreType"] = tlsTrustStoreType
        log["tlsTrustStorePath"] = tlsTrustStorePath
        log["tlsTrustStorePassword"] = HIDDEN
      }

      when (authentication) {
        is AuthenticationTokenConfig -> {
          authentication(AuthenticationFactory.token(authentication.token.value))
          log["AuthenticationFactory.token"] = HIDDEN
        }

        is AuthenticationAthenzConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationAthenz::class.java.name,
                  mapper.writeValueAsString(authentication),
              ),
          )
          log["AuthenticationFactory.AuthenticationAthenz"] = HIDDEN
        }

        is AuthenticationSaslConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationSasl::class.java.name,
                  mapper.writeValueAsString(authentication),
              ),
          )
          log["AuthenticationFactory.AuthenticationSasl"] = HIDDEN
        }

        is AuthenticationOAuth2Config -> {
          authentication(
              AuthenticationFactoryOAuth2.clientCredentials(
                  authentication.issuerUrl,
                  authentication.privateKey,
                  authentication.audience,
              ),
          )
          log["AuthenticationFactoryOAuth2.issuerUrl"] = authentication.issuerUrl
          log["AuthenticationFactoryOAuth2.privateKey"] = authentication.privateKey
          log["AuthenticationFactoryOAuth2.audience"] = authentication.audience
        }

        null -> Unit
      }
    }.build().also {
      logger.info {
        "Created PulsarClient with config: ${log.map { "${it.key}=${it.value}" }.joinToString()}"
      }
    }
  }

  /**
   * PulsarConfig builder (Useful for Java user)
   */
  class PulsarConfigBuilder {
    private val default = PulsarConfig(PULSAR_PROTOCOL, HTTP_PROTOCOL, UNSET, UNSET)
    private var tenant = default.tenant
    private var namespace = default.namespace
    private var brokerServiceUrl = default.brokerServiceUrl
    private var webServiceUrl = default.webServiceUrl
    private var allowedClusters = default.allowedClusters
    private var adminRoles = default.adminRoles
    private var tlsAllowInsecureConnection = default.tlsAllowInsecureConnection
    private var tlsEnableHostnameVerification = default.tlsEnableHostnameVerification
    private var tlsTrustCertsFilePath = default.tlsTrustCertsFilePath
    private var useKeyStoreTls = default.useKeyStoreTls
    private var tlsTrustStoreType = default.tlsTrustStoreType
    private var tlsTrustStorePath = default.tlsTrustStorePath
    private var tlsTrustStorePassword = default.tlsTrustStorePassword
    private var authentication = default.authentication
    private var policies = default.policies
    private var producer = default.producer
    private var consumer = default.consumer

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

    fun setProducer(producerConfig: ProducerConfig) =
        apply { this.producer = producerConfig }

    fun setConsumer(consumerConfig: ConsumerConfig) =
        apply { this.consumer = consumerConfig }

    fun build() = PulsarConfig(
        brokerServiceUrl.removeUnset(PULSAR_PROTOCOL),
        webServiceUrl.removeUnset(HTTP_PROTOCOL),
        tenant.removeUnset(),
        namespace.removeUnset(),
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
  }
}

private const val UNSET = "INFINITIC_UNSET_STRING"
private fun String.removeUnset(unset: String = UNSET): String = when (this) {
  unset -> ""
  else -> this
}

