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

package io.infinitic.pulsar.config

import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.transport.Transport
import io.infinitic.transport.pulsar.Pulsar

data class ClientConfig(
    /*
   Client name
    */
    val name: String? = null,

    /*
    Transport configuration
     */
    val transport: Transport = Transport.pulsar,

    /*
    Pulsar configuration
     */
    val pulsar: Pulsar? = null
) {
    init {
        if (transport == Transport.pulsar) {
            require(pulsar != null) { "Missing Pulsar configuration" }
        }
    }

    companion object {
        /**
         * Create ClientConfig from file in file system
         */
        @JvmStatic
        fun fromFile(vararg files: String): ClientConfig = loadConfigFromFile(files.toList())

        /**
         * Create ClientConfig from file in resources directory
         */
        @JvmStatic
        fun fromResource(vararg resources: String): ClientConfig = loadConfigFromResource(resources.toList())
    }
}
