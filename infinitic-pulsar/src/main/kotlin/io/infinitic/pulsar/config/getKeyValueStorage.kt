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

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.storage.StateStorage
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.storage.kodein.KodeinStorage
import io.infinitic.storage.redis.RedisStorage
import java.io.File

fun StateStorage.getKeyValueStorage(config: Config, type: String): KeyValueStorage = when (this) {
    StateStorage.inMemory -> InMemoryStorage()
    StateStorage.kodein -> getKodeinKeyValueStorage(config, type)
    StateStorage.redis -> RedisStorage(config.redis!!)
    StateStorage.pulsarState -> InMemoryStorage()
}

private fun getKodeinKeyValueStorage(config: Config, type: String): KodeinStorage {
    val path = "${config.kodein!!.path.trimEnd('/')}/$type/"
    val dir = File(path)
    if (!dir.exists()) dir.mkdirs()

    return KodeinStorage(config.kodein.copy(path = path))
}
