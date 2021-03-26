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
import java.util.UUID

/**
 * TagStateStorage implementations are responsible for storing the different state objects used by the engine.
 *
 * No assumptions are made on whether the storage should be persistent or not, nor how the data should be
 * transformed before being stored. These details are left to the different implementations.
 */
interface TagStateStorage {
    suspend fun getLastMessageId(tag: Tag, name: Name): MessageId?

    suspend fun setLastMessageId(tag: Tag, name: Name, messageId: MessageId)

    suspend fun getIds(tag: Tag, name: Name): Set<UUID>

    suspend fun addId(tag: Tag, name: Name, id: UUID)

    suspend fun removeId(tag: Tag, name: Name, id: UUID)
}
