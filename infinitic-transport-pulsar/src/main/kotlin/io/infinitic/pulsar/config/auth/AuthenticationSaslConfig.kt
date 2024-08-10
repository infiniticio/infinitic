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
package io.infinitic.pulsar.config.auth

import com.sksamuel.hoplite.Secret

data class AuthenticationSaslConfig(
  val tenantDomain: String,
  val tenantService: String,
  val providerDomain: String,
  val privateKey: Secret,
  val keyId: String = DEFAULT_KEY_ID,
) : ClientAuthenticationConfig() {
  /**
   * AuthenticationOAuth2Config builder (Useful for Java user)
   */
  @Suppress("unused")
  class Builder {
    private var tenantDomain: String? = null
    private var tenantService: String? = null
    private var providerDomain: String? = null
    private var privateKey: String? = null
    private var keyId: String? = null

    fun tenantDomain(tenantDomain: String) = apply { this.tenantDomain = tenantDomain }
    fun tenantService(tenantService: String) = apply { this.tenantService = tenantService }
    fun providerDomain(providerDomain: String) = apply { this.providerDomain = providerDomain }
    fun privateKey(privateKey: String) = apply { this.privateKey = privateKey }
    fun keyId(keyId: String) = apply { this.keyId = keyId }

    fun build() = AuthenticationSaslConfig(
        tenantDomain ?: throw IllegalArgumentException("tenantDomain must be set"),
        tenantService ?: throw IllegalArgumentException("tenantService must be set"),
        providerDomain ?: throw IllegalArgumentException("providerDomain must be set"),
        Secret(privateKey ?: throw IllegalArgumentException("privateKey must be set")),
        keyId ?: DEFAULT_KEY_ID,
    )
  }

  companion object {
    private const val DEFAULT_KEY_ID = "0"
  }
}
