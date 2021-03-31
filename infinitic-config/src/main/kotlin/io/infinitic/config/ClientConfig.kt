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

package io.infinitic.config

import io.infinitic.config.data.Pulsar
import io.infinitic.config.data.Task
import io.infinitic.config.data.Transport
import io.infinitic.config.data.Workflow

data class ClientConfig(
    /*
   Client name
    */
    @JvmField val name: String? = null,

    /*
    Transport configuration
     */
    @JvmField val transport: Transport = Transport.pulsar,

    /*
    Pulsar configuration
     */
    @JvmField val pulsar: Pulsar? = null,

    /*
    Tasks configuration (Used only inMemory)
     */
    @JvmField val tasks: List<Task> = listOf(),

    /*
    Workflows configuration (Used only inMemory)
     */
    @JvmField val workflows: List<Workflow> = listOf(),

) {
    init {
        if (transport == Transport.pulsar) {
            require(pulsar != null) { "Missing Pulsar configuration" }
        }
    }
}
