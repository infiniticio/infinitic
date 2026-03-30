/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.keySet

import io.infinitic.storage.Flushable

data class KeySetPage(
  val values: List<ByteArray>,
  val nextCursor: String?,
)

interface KeySetStorage : Flushable, AutoCloseable {
  /**
   * Retrieves the set of ByteArray associated with the given key.
   */
  suspend fun get(key: String): Set<ByteArray>

  /**
   * Retrieves one page of ByteArray associated with the given key.
   *
   * Implementations must return at most [limit] values.
   *
   * The cursor is backend-specific and opaque to callers. Implementations may encode buffered
   * state inside the cursor when the underlying backend pagination primitive does not natively
   * honor [limit].
   */
  suspend fun getPage(
    key: String,
    limit: Int,
    cursor: String? = null,
  ): KeySetPage

  /**
   * Adds a ByteArray value to the storage associated with the given key.
   */
  suspend fun add(key: String, value: ByteArray)

  /**
   * Removes a ByteArray value from the storage associated with the given key.
   */
  suspend fun remove(key: String, value: ByteArray)

  /**
   * Retrieves the set of ByteArray associated with the given keys.
   */
  suspend fun get(keys: Set<String>): Map<String, Set<ByteArray>>

  /**
   * Updates the storage by adding and/or removing sets of ByteArray associated with their respective keys.
   */
  suspend fun update(
    add: Map<String, Set<ByteArray>> = mapOf(),
    remove: Map<String, Set<ByteArray>> = mapOf()
  )
}
