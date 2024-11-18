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
package io.infinitic.storage.keyValue

import io.infinitic.storage.Flushable

interface KeyValueStorage : Flushable, AutoCloseable {
  /**
   * Retrieves the value associated with the specified key from the storage.
   */
  suspend fun get(key: String): ByteArray?

  /**
   * Stores the given byte array value associated with the specified key.
   * If the value is null, it should delete the corresponding key.
   */
  suspend fun put(key: String, bytes: ByteArray?)

  /**
   * Retrieves the values associated with the specified keys from the storage.
   */
  suspend fun get(keys: Set<String>): Map<String, ByteArray?>

  /**
   * Stores multiple key-value pairs in the storage system.
   * - If the value is null, it should delete the corresponding key.
   * - This operation MUST be atomic
   */
  suspend fun put(bytes: Map<String, ByteArray?>)
}
