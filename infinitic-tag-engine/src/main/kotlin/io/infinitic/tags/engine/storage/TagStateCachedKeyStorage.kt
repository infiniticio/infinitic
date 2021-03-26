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
import io.infinitic.common.storage.keySet.KeySetCache
import io.infinitic.common.storage.keySet.KeySetStorage
import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tags.data.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class TagStateCachedKeyStorage(
    private val keyValueStorage: KeyValueStorage,
    private val keyValueCache: KeyValueCache<MessageId>,
    private val keySetStorage: KeySetStorage,
    private val keySetCache: KeySetCache<UUID>
) : TagStateStorage {

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getLastMessageId(tag: Tag, name: Name): MessageId? {
        val key = getTagMessageIdKey(tag, name)

        val messageId = keyValueCache.getValue(key) ?: run {
            logger.debug("tag {} - name {} - getLastMessageId - absent from cache, get from storage", tag, name)
            keyValueStorage.getValue(key)
                ?.let { MessageId.fromByteArray(it) }
                ?.also { keyValueCache.putValue(key, it) }
        }
        logger.debug("tag {} - getLastMessageId {}", tag, messageId)

        return messageId
    }

    override suspend fun setLastMessageId(tag: Tag, name: Name, messageId: MessageId) {
        val key = getTagMessageIdKey(tag, name)
        keyValueCache.putValue(key, messageId)
        keyValueStorage.putValue(key, messageId.toByteArray())
        logger.debug("tag {} - name {} - setLastMessageId {}", tag, name, messageId)
    }

    override suspend fun getIds(tag: Tag, name: Name): Set<UUID> {
        val key = getTagSetIdsKey(tag, name)
        val set = keySetCache.getSet(key) ?: run {
            logger.debug("tag {} - name {} - getIds - absent from cache, get from storage", tag, name)
            keySetStorage.getSet(key)
                ?.map { it.toUUID() }?.toSet()
                ?.also { keySetCache.setSet(key, it) }
        }
        logger.debug("tag {} - name {} - getIds count {}", tag, name, set?.size ?: 0)

        return set ?: setOf()
    }

    override suspend fun addId(tag: Tag, name: Name, id: UUID) {
        val key = getTagSetIdsKey(tag, name)
        keySetCache.addToSet(key, id)
        keySetStorage.addToSet(key, id.toByteArray())
        logger.debug("tag {} - name {} - addId {}", tag, name, id)
    }

    override suspend fun removeId(tag: Tag, name: Name, id: UUID) {
        val key = getTagSetIdsKey(tag, name)
        keySetCache.removeFromSet(key, id)
        keySetStorage.removeFromSet(key, id.toByteArray())
        logger.debug("tag {} - name {} - removeId {}", tag, name, id)
    }

    private fun getTagMessageIdKey(tag: Tag, name: Name) = "name:$name:tag:$tag:messageId"

    private fun getTagSetIdsKey(tag: Tag, name: Name) = "name:$name:tag:$tag:setIds"

    /*
    Use for tests
     */
    fun flush() {
        if (keyValueStorage is Flushable) {
            keyValueStorage.flush()
        } else {
            throw RuntimeException("Storage non flushable")
        }
        if (keyValueCache is Flushable) {
            keyValueCache.flush()
        } else {
            throw Exception("Cache non flushable")
        }
    }
}
