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

package io.infinitic.tags.engine.storage

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.Name
import io.infinitic.common.data.UUIDConversion.toByteArray
import io.infinitic.common.data.UUIDConversion.toUUID
import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keySet.KeySetStorage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tags.data.Tag
import java.util.UUID

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
class BinaryTagStateStorage(
    private val keyValueStorage: KeyValueStorage,
    private val keySetStorage: KeySetStorage,
) : TagStateStorage, Flushable {

    override suspend fun getLastMessageId(tag: Tag, name: Name): MessageId? {
        val key = getTagMessageIdKey(tag, name)

        return keyValueStorage.getValue(key)
            ?.let { MessageId.fromByteArray(it) }
    }

    override suspend fun setLastMessageId(tag: Tag, name: Name, messageId: MessageId) {
        val key = getTagMessageIdKey(tag, name)
        keyValueStorage.putValue(key, messageId.toByteArray())
    }

    override suspend fun getIds(tag: Tag, name: Name): Set<UUID> {
        val key = getTagSetIdsKey(tag, name)
        return keySetStorage.getSet(key)
            ?.map { it.toUUID() }?.toSet()
            ?: setOf()
    }

    override suspend fun addId(tag: Tag, name: Name, id: UUID) {
        val key = getTagSetIdsKey(tag, name)
        keySetStorage.addToSet(key, id.toByteArray())
    }

    override suspend fun removeId(tag: Tag, name: Name, id: UUID) {
        val key = getTagSetIdsKey(tag, name)
        keySetStorage.removeFromSet(key, id.toByteArray())
    }

    private fun getTagMessageIdKey(tag: Tag, name: Name) = "name**$name**tag**$tag**messageId"

    private fun getTagSetIdsKey(tag: Tag, name: Name) = "name**$name**tag**$tag**setIds"

    override fun flush() {
        keyValueStorage.flush()
        keySetStorage.flush()
    }
}
