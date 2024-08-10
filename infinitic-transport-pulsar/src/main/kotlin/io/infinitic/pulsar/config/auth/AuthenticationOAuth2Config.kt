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

import java.net.URL

data class AuthenticationOAuth2Config(
  val issuerUrl: URL,
  val privateKey: URL,
  val audience: String
) : ClientAuthenticationConfig() {

  /**
   * AuthenticationOAuth2Config builder (Useful for Java user)
   */
  @Suppress("unused")
  class Builder {
    private var issuerUrl: URL? = null
    private var privateKey: URL? = null
    private var audience: String? = null

    fun issuerUrl(issuerUrl: URL) = apply { this.issuerUrl = issuerUrl }
    fun privateKey(privateKey: URL) = apply { this.privateKey = privateKey }
    fun audience(audience: String) = apply { this.audience = audience }

    fun build() = AuthenticationOAuth2Config(
        issuerUrl ?: throw IllegalArgumentException("issuerUrl must be set"),
        privateKey ?: throw IllegalArgumentException("privateKey must be set"),
        audience ?: throw IllegalArgumentException("audience must be set"),
    )
  }
}
