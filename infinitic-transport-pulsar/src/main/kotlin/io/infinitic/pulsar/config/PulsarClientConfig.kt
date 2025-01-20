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
import io.infinitic.pulsar.admin.AdminConfigInterface
import io.infinitic.pulsar.client.ClientConfigInterface
import io.infinitic.pulsar.config.auth.ClientAuthenticationConfig

@Suppress("unused")
data class PulsarClientConfig(
  override val listenerName: String? = null,
  override val connectionMaxIdleSeconds: Int? = null,
  override val authentication: ClientAuthenticationConfig? = null,
  override val operationTimeoutSeconds: Double? = null,
  override val lookupTimeoutSeconds: Double? = null,
  override val ioThreads: Int? = null,
  override val listenerThreads: Int? = null,
  override val connectionsPerBroker: Int? = null,
  override val enableTcpNoDelay: Boolean? = null,
  override val tlsKeyFilePath: String? = null,
  override val tlsCertificateFilePath: String? = null,
  override val tlsTrustCertsFilePath: String? = null,
  override val allowTlsInsecureConnection: Boolean? = null,
  override val enableTlsHostnameVerification: Boolean? = null,
  override val useKeyStoreTls: Boolean? = null,
  override val sslProvider: String? = null,
  override val tlsKeyStoreType: String? = null,
  override val tlsKeyStorePath: String? = null,
  override val tlsKeyStorePassword: String? = null,
  override val tlsTrustStoreType: String? = null,
  override val tlsTrustStorePath: String? = null,
  override val tlsTrustStorePassword: Secret? = null,
  override val tlsCiphers: Set<String>? = null,
  override val tlsProtocols: Set<String>? = null,
  override val memoryLimitMB: Long? = null,
  override val statsIntervalSeconds: Double? = null,
  override val maxConcurrentLookupRequests: Int? = null,
  override val maxLookupRequests: Int? = null,
  override val maxLookupRedirects: Int? = null,
  override val maxNumberOfRejectedRequestPerConnection: Int? = null,
  override val keepAliveIntervalSeconds: Double? = null,
  override val connectionTimeoutSeconds: Double? = null,
  override val startingBackoffIntervalSeconds: Double? = null,
  override val maxBackoffIntervalSeconds: Double? = null,
  override val enableBusyWait: Boolean? = null,
//val clock(clock: Clock?): ClientBuilder?
//val proxyServiceUrl(proxyServiceUrl: String?, proxyProtocol: ProxyProtocol?): ClientBuilder?
  override val enableTransaction: Boolean? = null,
//val dnsLookupBind(address: String?, port: Int): ClientBuilder?
//val socks5ProxyAddress(socks5ProxyAddress: InetSocketAddress?): ClientBuilder?
  override val socks5ProxyUsername: String? = null,
  override val socks5ProxyPassword: Secret? = null,
    // Admin only
  override val readTimeoutSeconds: Double? = null,
  override val requestTimeoutSeconds: Double? = null,
  override val autoCertRefreshTime: Double? = null,
) : ClientConfigInterface, AdminConfigInterface {
  companion object {
    @JvmStatic
    fun builder() = ClientConfigBuilder()
  }

  /**
   * ClientConfig builder
   */
  class ClientConfigBuilder {
    private var serviceUrl: String? = null
    private var listenerName: String? = null
    private var connectionMaxIdleSeconds: Int? = null
    private var authentication: ClientAuthenticationConfig? = null
    private var operationTimeoutSeconds: Double? = null
    private var lookupTimeoutSeconds: Double? = null
    private var ioThreads: Int? = null
    private var listenerThreads: Int? = null
    private var connectionsPerBroker: Int? = null
    private var enableTcpNoDelay: Boolean? = null
    private var tlsKeyFilePath: String? = null
    private var tlsCertificateFilePath: String? = null
    private var tlsTrustCertsFilePath: String? = null
    private var allowTlsInsecureConnection: Boolean? = null
    private var enableTlsHostnameVerification: Boolean? = null
    private var useKeyStoreTls: Boolean? = null
    private var sslProvider: String? = null
    private var tlsKeyStoreType: String? = null
    private var tlsKeyStorePath: String? = null
    private var tlsKeyStorePassword: String? = null
    private var tlsTrustStoreType: String? = null
    private var tlsTrustStorePath: String? = null
    private var tlsTrustStorePassword: Secret? = null
    private var tlsCiphers: Set<String>? = null
    private var tlsProtocols: Set<String>? = null
    private var memoryLimitMB: Long? = null
    private var statsIntervalSeconds: Double? = null
    private var maxConcurrentLookupRequests: Int? = null
    private var maxLookupRequests: Int? = null
    private var maxLookupRedirects: Int? = null
    private var maxNumberOfRejectedRequestPerConnection: Int? = null
    private var keepAliveIntervalSeconds: Double? = null
    private var connectionTimeoutSeconds: Double? = null
    private var startingBackoffIntervalSeconds: Double? = null
    private var maxBackoffIntervalSeconds: Double? = null
    private var enableBusyWait: Boolean? = null
    private var enableTransaction: Boolean? = null
    private var socks5ProxyUsername: String? = null
    private var socks5ProxyPassword: Secret? = null
    private var readTimeoutSeconds: Double? = null
    private var requestTimeoutSeconds: Double? = null
    private var autoCertRefreshTime: Double? = null

    fun setServiceUrl(serviceUrl: String) =
        apply { this.serviceUrl = serviceUrl }

    fun setListenerName(listenerName: String) =
        apply { this.listenerName = listenerName }

    fun setConnectionMaxIdleSeconds(connectionMaxIdleSeconds: Int) =
        apply { this.connectionMaxIdleSeconds = connectionMaxIdleSeconds }

    fun setAuthentication(authentication: ClientAuthenticationConfig) =
        apply { this.authentication = authentication }

    fun setOperationTimeoutSeconds(operationTimeoutSeconds: Double) =
        apply { this.operationTimeoutSeconds = operationTimeoutSeconds }

    fun setLookupTimeoutSeconds(lookupTimeoutSeconds: Double) =
        apply { this.lookupTimeoutSeconds = lookupTimeoutSeconds }

    fun setIoThreads(ioThreads: Int) =
        apply { this.ioThreads = ioThreads }

    fun setListenerThreads(listenerThreads: Int) =
        apply { this.listenerThreads = listenerThreads }

    fun setConnectionsPerBroker(connectionsPerBroker: Int) =
        apply { this.connectionsPerBroker = connectionsPerBroker }

    fun setEnableTcpNoDelay(enableTcpNoDelay: Boolean) =
        apply { this.enableTcpNoDelay = enableTcpNoDelay }

    fun setTlsKeyFilePath(tlsKeyFilePath: String) =
        apply { this.tlsKeyFilePath = tlsKeyFilePath }

    fun setTlsCertificateFilePath(tlsCertificateFilePath: String) =
        apply { this.tlsCertificateFilePath = tlsCertificateFilePath }

    fun setTlsTrustCertsFilePath(tlsTrustCertsFilePath: String) =
        apply { this.tlsTrustCertsFilePath = tlsTrustCertsFilePath }

    fun setAllowTlsInsecureConnection(allowTlsInsecureConnection: Boolean) =
        apply { this.allowTlsInsecureConnection = allowTlsInsecureConnection }

    fun setEnableTlsHostnameVerification(enableTlsHostnameVerification: Boolean) =
        apply { this.enableTlsHostnameVerification = enableTlsHostnameVerification }

    fun setUseKeyStoreTls(useKeyStoreTls: Boolean) =
        apply { this.useKeyStoreTls = useKeyStoreTls }

    fun setSslProvider(sslProvider: String) =
        apply { this.sslProvider = sslProvider }

    fun setTlsKeyStoreType(tlsKeyStoreType: String) =
        apply { this.tlsKeyStoreType = tlsKeyStoreType }

    fun setTlsKeyStorePath(tlsKeyStorePath: String) =
        apply { this.tlsKeyStorePath = tlsKeyStorePath }

    fun setTlsKeyStorePassword(tlsKeyStorePassword: String) =
        apply { this.tlsKeyStorePassword = tlsKeyStorePassword }

    fun setTlsTrustStoreType(tlsTrustStoreType: String) =
        apply { this.tlsTrustStoreType = tlsTrustStoreType }

    fun setTlsTrustStorePath(tlsTrustStorePath: String) =
        apply { this.tlsTrustStorePath = tlsTrustStorePath }

    fun setTlsTrustStorePassword(tlsTrustStorePassword: String) =
        apply { this.tlsTrustStorePassword = Secret(tlsTrustStorePassword) }

    fun setTlsCiphers(tlsCiphers: Set<String>) =
        apply { this.tlsCiphers = tlsCiphers }

    fun setTlsProtocols(tlsProtocols: Set<String>) =
        apply { this.tlsProtocols = tlsProtocols }

    fun setMemoryLimitMB(memoryLimitMB: Long) =
        apply { this.memoryLimitMB = memoryLimitMB }

    fun setStatsIntervalSeconds(statsIntervalSeconds: Double) =
        apply { this.statsIntervalSeconds = statsIntervalSeconds }

    fun setMaxConcurrentLookupRequests(maxConcurrentLookupRequests: Int) =
        apply { this.maxConcurrentLookupRequests = maxConcurrentLookupRequests }

    fun setMaxLookupRequests(maxLookupRequests: Int) =
        apply { this.maxLookupRequests = maxLookupRequests }

    fun setMaxLookupRedirects(maxLookupRedirects: Int) =
        apply { this.maxLookupRedirects = maxLookupRedirects }

    fun setMaxNumberOfRejectedRequestPerConnection(maxNumberOfRejectedRequestPerConnection: Int) =
        apply {
          this.maxNumberOfRejectedRequestPerConnection = maxNumberOfRejectedRequestPerConnection
        }

    fun setKeepAliveIntervalSeconds(keepAliveIntervalSeconds: Double) =
        apply { this.keepAliveIntervalSeconds = keepAliveIntervalSeconds }

    fun setConnectionTimeoutSeconds(connectionTimeoutSeconds: Double) =
        apply { this.connectionTimeoutSeconds = connectionTimeoutSeconds }

    fun setStartingBackoffIntervalSeconds(startingBackoffIntervalSeconds: Double) =
        apply { this.startingBackoffIntervalSeconds = startingBackoffIntervalSeconds }

    fun setMaxBackoffIntervalSeconds(maxBackoffIntervalSeconds: Double) =
        apply { this.maxBackoffIntervalSeconds = maxBackoffIntervalSeconds }

    fun setEnableBusyWait(enableBusyWait: Boolean) =
        apply { this.enableBusyWait = enableBusyWait }

    fun setEnableTransaction(enableTransaction: Boolean) =
        apply { this.enableTransaction = enableTransaction }

    fun setSocks5ProxyUsername(socks5ProxyUsername: String) =
        apply { this.socks5ProxyUsername = socks5ProxyUsername }

    fun setSocks5ProxyPassword(socks5ProxyPassword: String) =
        apply { this.socks5ProxyPassword = Secret(socks5ProxyPassword) }

    fun setReadTimeoutSeconds(readTimeoutSeconds: Double) =
        apply { this.readTimeoutSeconds = readTimeoutSeconds }

    fun setRequestTimeoutSeconds(requestTimeoutSeconds: Double) =
        apply { this.requestTimeoutSeconds = requestTimeoutSeconds }

    fun setAutoCertRefreshTime(autoCertRefreshTime: Double) =
        apply { this.autoCertRefreshTime = autoCertRefreshTime }

    fun build(): PulsarClientConfig {
      return PulsarClientConfig(
          listenerName = listenerName,
          connectionMaxIdleSeconds = connectionMaxIdleSeconds,
          authentication = authentication,
          operationTimeoutSeconds = operationTimeoutSeconds,
          lookupTimeoutSeconds = lookupTimeoutSeconds,
          ioThreads = ioThreads,
          listenerThreads = listenerThreads,
          connectionsPerBroker = connectionsPerBroker,
          enableTcpNoDelay = enableTcpNoDelay,
          tlsKeyFilePath = tlsKeyFilePath,
          tlsCertificateFilePath = tlsCertificateFilePath,
          tlsTrustCertsFilePath = tlsTrustCertsFilePath,
          allowTlsInsecureConnection = allowTlsInsecureConnection,
          enableTlsHostnameVerification = enableTlsHostnameVerification,
          useKeyStoreTls = useKeyStoreTls,
          sslProvider = sslProvider,
          tlsKeyStoreType = tlsKeyStoreType,
          tlsKeyStorePath = tlsKeyStorePath,
          tlsKeyStorePassword = tlsKeyStorePassword,
          tlsTrustStoreType = tlsTrustStoreType,
          tlsTrustStorePath = tlsTrustStorePath,
          tlsTrustStorePassword = tlsTrustStorePassword,
          tlsCiphers = tlsCiphers,
          tlsProtocols = tlsProtocols,
          memoryLimitMB = memoryLimitMB,
          statsIntervalSeconds = statsIntervalSeconds,
          maxConcurrentLookupRequests = maxConcurrentLookupRequests,
          maxLookupRequests = maxLookupRequests,
          maxLookupRedirects = maxLookupRedirects,
          maxNumberOfRejectedRequestPerConnection = maxNumberOfRejectedRequestPerConnection,
          keepAliveIntervalSeconds = keepAliveIntervalSeconds,
          connectionTimeoutSeconds = connectionTimeoutSeconds,
          startingBackoffIntervalSeconds = startingBackoffIntervalSeconds,
          maxBackoffIntervalSeconds = maxBackoffIntervalSeconds,
          enableBusyWait = enableBusyWait,
          enableTransaction = enableTransaction,
          socks5ProxyUsername = socks5ProxyUsername,
          socks5ProxyPassword = socks5ProxyPassword,
          readTimeoutSeconds = readTimeoutSeconds,
          requestTimeoutSeconds = requestTimeoutSeconds,
          autoCertRefreshTime = autoCertRefreshTime,
      )
    }
  }
}
