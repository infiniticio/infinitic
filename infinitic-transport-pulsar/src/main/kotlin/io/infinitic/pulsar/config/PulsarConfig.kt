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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.config.notNullPropertiesToString
import io.infinitic.pulsar.admin.AdminConfigInterface
import io.infinitic.pulsar.admin.InfiniticPulsarAdmin
import io.infinitic.pulsar.client.ClientConfigInterface
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.auth.AuthenticationAthenzConfig
import io.infinitic.pulsar.config.auth.AuthenticationOAuth2Config
import io.infinitic.pulsar.config.auth.AuthenticationSaslConfig
import io.infinitic.pulsar.config.auth.AuthenticationTokenConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.serDe.java.Json.mapper
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.api.AuthenticationFactory
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SizeUnit
import org.apache.pulsar.client.impl.auth.oauth2.AuthenticationFactoryOAuth2
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.auth.AuthenticationAthenz as PulsarAuthenticationAthenz
import org.apache.pulsar.client.impl.auth.AuthenticationSasl as PulsarAuthenticationSasl

@Suppress("unused")
data class PulsarConfig(
  val brokerServiceUrl: String, // "pulsar://localhost:6650/",
  val webServiceUrl: String, // "http://localhost:8080",
  val tenant: String,
  val namespace: String,
  val allowedClusters: Set<String>? = null,
  val adminRoles: Set<String>? = null,
  val client: PulsarClientConfig = PulsarClientConfig(),
  val policies: PoliciesConfig = PoliciesConfig(),
  val producer: PulsarProducerConfig = PulsarProducerConfig(),
  val consumer: PulsarConsumerConfig = PulsarConsumerConfig()
) {

  companion object {

    private val logger = KotlinLogging.logger {}
    private const val PULSAR_PROTOCOL = "pulsar://"
    private const val PULSAR_PROTOCOL_SSL = "pulsar+ssl://"
    private const val HTTP_PROTOCOL = "http://"
    private const val HTTP_PROTOCOL_SSL = "https://"

    /**
     * Create PulsarConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): PulsarConfig =
        loadFromYamlFile(*files)

    /**
     * Create PulsarConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): PulsarConfig =
        loadFromYamlResource(*resources)

    /**
     * Create PulsarConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): PulsarConfig =
        loadFromYamlString(*yamls)
  }

  init {
    require(
        brokerServiceUrl.startsWith(PULSAR_PROTOCOL) ||
            brokerServiceUrl.startsWith(PULSAR_PROTOCOL_SSL),
    ) {
      when {
        brokerServiceUrl.isBlank() -> "brokerServiceUrl can NOT be blank. If you're using a " +
            "local Pulsar instance, the value is typically set to pulsar://localhost:6650/"

        else -> "The brokerServiceUrl value MUST start with pulsar:// or pulsar+ssl://"
      }
    }

    require(
        webServiceUrl.startsWith(HTTP_PROTOCOL) ||
            webServiceUrl.startsWith(HTTP_PROTOCOL_SSL),
    ) {
      when {
        webServiceUrl.isBlank() -> "webServiceUrl can NOT be blank. If you're using a " +
            "local Pulsar instance, the value is typically set to http://localhost:8080"

        else -> "The webServiceUrl value MUST start with http:// or https://"
      }
    }

    require(tenant.isNotBlank()) { "tenant can NOT be blank" }

    require(namespace.isNotBlank()) { "namespace can NOT be blank" }
  }

  val infiniticPulsarClient by lazy { InfiniticPulsarClient(pulsarClient) }
  val infiniticPulsarAdmin by lazy { InfiniticPulsarAdmin(pulsarAdmin) }
  val pulsarResources by lazy { PulsarResources(this) }

  val pulsarAdmin: PulsarAdmin by lazy {
    val admin: AdminConfigInterface = client
    PulsarAdmin.builder().apply {

      serviceHttpUrl(webServiceUrl)

      when (val auth = client.authentication) {
        is AuthenticationTokenConfig -> {
          authentication(AuthenticationFactory.token(auth.token.value))
        }

        is AuthenticationAthenzConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationAthenz::class.java.name,
                  mapper.writeValueAsString(auth),
              ),
          )
        }

        is AuthenticationSaslConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationSasl::class.java.name,
                  mapper.writeValueAsString(auth),
              ),
          )
        }

        is AuthenticationOAuth2Config -> {
          authentication(
              AuthenticationFactoryOAuth2.clientCredentials(
                  auth.issuerUrl,
                  auth.privateKey,
                  auth.audience,
              ),
          )
        }

        null -> Unit
      }

      admin.tlsKeyFilePath?.let {
        tlsKeyFilePath(it)
      }

      admin.tlsCertificateFilePath?.let {
        tlsCertificateFilePath(it)
      }

      admin.tlsTrustCertsFilePath?.let {
        tlsTrustCertsFilePath(it)
      }

      admin.allowTlsInsecureConnection?.let {
        allowTlsInsecureConnection(it)
      }

      admin.enableTlsHostnameVerification?.let {
        enableTlsHostnameVerification(it)
      }

      admin.useKeyStoreTls?.let {
        useKeyStoreTls(it)
      }

      admin.sslProvider?.let {
        sslProvider(it)
      }

      admin.tlsKeyStoreType?.let {
        tlsKeyStoreType(it)
      }

      admin.tlsKeyStorePath?.let {
        tlsKeyStorePath(it)
      }

      admin.tlsKeyStorePassword?.let {
        tlsKeyStorePassword(it)
      }

      admin.tlsTrustStoreType?.let {
        tlsTrustStoreType(it)
      }

      admin.tlsTrustStorePath?.let {
        tlsTrustStorePath(it)
      }

      admin.tlsTrustStorePassword?.let {
        tlsTrustStorePassword(it.value)
      }

      admin.tlsCiphers?.let {
        tlsCiphers(it)
      }

      admin.tlsProtocols?.let {
        tlsProtocols(it)
      }

      admin.connectionTimeoutSeconds?.let {
        connectionTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      admin.readTimeoutSeconds?.let {
        readTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      admin.requestTimeoutSeconds?.let {
        requestTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      admin.autoCertRefreshTime?.let {
        autoCertRefreshTime((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

    }.build().also {
      logger.info {
        "Created PulsarAdmin with config: webServiceUrl=$webServiceUrl, " +
            admin.notNullPropertiesToString(AdminConfigInterface::class)
      }
    }
  }


  val pulsarClient: PulsarClient by lazy {
    PulsarClient.builder().apply {
      serviceUrl(brokerServiceUrl)

      client.listenerName?.let {
        listenerName(it)
      }

      client.connectionMaxIdleSeconds?.let {
        connectionMaxIdleSeconds(it)
      }

      when (val auth = client.authentication) {
        is AuthenticationTokenConfig -> {
          authentication(AuthenticationFactory.token(auth.token.value))
        }

        is AuthenticationAthenzConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationAthenz::class.java.name,
                  mapper.writeValueAsString(auth),
              ),
          )
        }

        is AuthenticationSaslConfig -> {
          authentication(
              AuthenticationFactory.create(
                  PulsarAuthenticationSasl::class.java.name,
                  mapper.writeValueAsString(auth),
              ),
          )
        }

        is AuthenticationOAuth2Config -> {
          authentication(
              AuthenticationFactoryOAuth2.clientCredentials(
                  auth.issuerUrl,
                  auth.privateKey,
                  auth.audience,
              ),
          )
        }

        null -> Unit
      }

      client.operationTimeoutSeconds?.let {
        operationTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      client.lookupTimeoutSeconds?.let {
        lookupTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      client.ioThreads?.let {
        ioThreads(it)
      }

      client.listenerThreads?.let {
        listenerThreads(it)
      }

      client.connectionsPerBroker?.let {
        connectionsPerBroker(it)
      }

      client.enableTcpNoDelay?.let {
        enableTcpNoDelay(it)
      }

      client.tlsKeyFilePath?.let {
        tlsKeyFilePath(it)
      }

      client.tlsCertificateFilePath?.let {
        tlsCertificateFilePath(it)
      }

      client.tlsTrustCertsFilePath?.let {
        tlsTrustCertsFilePath(it)
      }

      client.allowTlsInsecureConnection?.let {
        allowTlsInsecureConnection(it)
      }

      client.enableTlsHostnameVerification?.let {
        enableTlsHostnameVerification(it)
      }

      client.useKeyStoreTls?.let {
        useKeyStoreTls(it)
      }

      client.sslProvider?.let {
        sslProvider(it)
      }

      client.tlsKeyStoreType?.let {
        tlsKeyStoreType(it)
      }

      client.tlsKeyStorePath?.let {
        tlsKeyStorePath(it)
      }

      client.tlsKeyStorePassword?.let {
        tlsKeyStorePassword(it)
      }

      client.tlsTrustStoreType?.let {
        tlsTrustStoreType(it)
      }

      client.tlsTrustStorePath?.let {
        tlsTrustStorePath(it)
      }

      client.tlsTrustStorePassword?.let {
        tlsTrustStorePassword(it.value)
      }

      client.tlsCiphers?.let {
        tlsCiphers(it)
      }

      client.tlsProtocols?.let {
        tlsProtocols(it)
      }

      client.memoryLimitMB?.let {
        memoryLimit(it, SizeUnit.MEGA_BYTES)
      }

      client.statsIntervalSeconds?.let {
        statsInterval((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }

      client.maxConcurrentLookupRequests?.let {
        maxConcurrentLookupRequests(it)
      }

      client.maxLookupRequests?.let {
        maxLookupRequests(it)
      }

      client.maxLookupRedirects?.let {
        maxLookupRedirects(it)
      }

      client.maxNumberOfRejectedRequestPerConnection?.let {
        maxNumberOfRejectedRequestPerConnection(it)
      }

      client.keepAliveIntervalSeconds?.let {
        keepAliveInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      client.connectionTimeoutSeconds?.let {
        connectionTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }

      client.startingBackoffIntervalSeconds?.let {
        startingBackoffInterval((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }

      client.maxBackoffIntervalSeconds?.let {
        startingBackoffInterval((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }

      client.enableBusyWait?.let {
        enableBusyWait(it)
      }

      client.enableTransaction?.let {
        enableTransaction(it)
      }

      client.socks5ProxyUsername?.let {
        socks5ProxyUsername(it)
      }

      client.socks5ProxyPassword?.let {
        socks5ProxyPassword(it.value)
      }
    }.build().also {
      logger.info {
        "Created PulsarClient with config: brokerServiceUrl=$brokerServiceUrl, " +
            client.notNullPropertiesToString(ClientConfigInterface::class)
      }
    }
  }
}


