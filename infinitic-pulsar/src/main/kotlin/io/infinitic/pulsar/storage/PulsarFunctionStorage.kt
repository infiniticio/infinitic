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

package io.infinitic.pulsar.storage

import io.infinitic.common.storage.keyValue.KeyValueStorage
import org.apache.pulsar.functions.api.Context
import java.nio.ByteBuffer

class PulsarFunctionStorage(private val context: Context) : KeyValueStorage {
    override suspend fun getValue(key: String): ByteArray? =
        context.getState(key).array()

    override suspend fun putValue(key: String, value: ByteArray) =
        context.putState(key, ByteBuffer.wrap(value))

    override suspend fun delValue(key: String) =
        context.deleteState(key)

    override fun flush() {
        // flush is used in tests, no actual implementation needed here
    }
}
