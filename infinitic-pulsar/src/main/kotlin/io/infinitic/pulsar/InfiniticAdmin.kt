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

package io.infinitic.pulsar

import io.infinitic.pulsar.admin.initInfinitic
import io.infinitic.pulsar.config.AdminConfig
import io.infinitic.pulsar.config.loadConfigFromFile
import io.infinitic.pulsar.config.loadConfigFromResource
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.admin.PulsarAdmin

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticAdmin(
    @JvmField val pulsarAdmin: PulsarAdmin,
    @JvmField val tenant: String,
    @JvmField val namespace: String,
    @JvmField val allowedClusters: Set<String>? = null
) {
    companion object {
        /*
        Create InfiniticAdmin from an AdminConfig
         */
        @JvmStatic
        fun fromConfig(config: AdminConfig): InfiniticAdmin {
            // build PulsarAdmin from config
            val pulsarAdmin = PulsarAdmin
                .builder()
                .serviceHttpUrl(config.pulsar.serviceHttpUrl)
                .allowTlsInsecureConnection(true)
                .build()

            return InfiniticAdmin(
                pulsarAdmin,
                config.pulsar.tenant,
                config.pulsar.namespace,
                config.pulsar.allowedClusters
            )
        }

        /*
        Create InfiniticAdmin from an AdminConfig loaded from a resource
         */
        @JvmStatic
        fun fromResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
       Create InfiniticAdmin from an AdminConfig loaded from a file
        */
        @JvmStatic
        fun fromFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    fun init() = runBlocking { pulsarAdmin.initInfinitic(tenant, namespace, allowedClusters) }

    fun close() = pulsarAdmin.close()
}
