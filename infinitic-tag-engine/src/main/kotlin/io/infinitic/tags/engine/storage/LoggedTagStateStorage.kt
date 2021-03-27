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
import io.infinitic.common.tags.data.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * TagStateStorage implementations are responsible for storing the different state objects used by the engine.
 *
 * No assumptions are made on whether the storage should be persistent or not, nor how the data should be
 * transformed before being stored. These details are left to the different implementations.
 */
class LoggedTagStateStorage(val storage: TagStateStorage) : TagStateStorage by storage {

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getLastMessageId(tag: Tag, name: Name): MessageId? {
        val messageId = storage.getLastMessageId(tag, name)
        logger.debug("tag {} - name {} - getLastMessageId {}", tag, name, messageId)

        return messageId
    }

    override suspend fun setLastMessageId(tag: Tag, name: Name, messageId: MessageId) {
        logger.debug("tag {} - name {} - setLastMessageId {}", tag, name, messageId)
        storage.setLastMessageId(tag, name, messageId)
    }

    override suspend fun getIds(tag: Tag, name: Name): Set<UUID> {
        val ids = storage.getIds(tag, name)
        logger.debug("tag {} - name {} - ids.size {}", tag, name, ids.size)

        return ids
    }

    override suspend fun addId(tag: Tag, name: Name, id: UUID) {
        logger.debug("tag {} - name {} - addId {}", tag, name, id)
        storage.addId(tag, name, id)
    }

    override suspend fun removeId(tag: Tag, name: Name, id: UUID) {
        logger.debug("tag {} - name {} - removeId {}", tag, name, id)
        storage.removeId(tag, name, id)
    }
}
