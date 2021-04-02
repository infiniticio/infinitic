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

package io.infinitic.storage.redis

import redis.clients.jedis.Protocol

data class Redis(
    @JvmField val host: String = Protocol.DEFAULT_HOST,
    @JvmField var port: Int = Protocol.DEFAULT_PORT,
    @JvmField var timeout: Int = Protocol.DEFAULT_TIMEOUT,
    @JvmField var user: String? = null,
    @JvmField var password: String? = null,
    @JvmField var database: Int = Protocol.DEFAULT_DATABASE,
    @JvmField var ssl: Boolean = false
)
