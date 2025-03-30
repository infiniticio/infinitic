/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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

import org.jetbrains.annotations.TestOnly
import java.io.Closeable

interface KeyValueStorage : Closeable {
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

  /**
   * Updates the value only if the current version matches the expected version.
   * @return true if the update was successful, false if the version didn't match
   */
  suspend fun putWithVersion(key: String, bytes: ByteArray?, expectedVersion: Long): Boolean

  /**
   * Retrieves  both value and version associated with the specified key from the storage
   * @return Pair of value and version, where version is 0 if value is null
   */
  suspend fun getStateAndVersion(key: String): Pair<ByteArray?, Long>

  /**
   * Retrieves both values and versions for multiple keys from the storage
   * @return Map of keys to pairs of value and version, where version is 0 if value is null
   */
  suspend fun getStatesAndVersions(keys: Set<String>): Map<String, Pair<ByteArray?, Long>>

  /**
   * Updates multiple key-value pairs only if their current versions match the expected versions.
   * - This operation MUST be atomic: either all updates succeed, or none do
   * - If any version check fails, no updates should be performed
   * @param updates Map of keys to pairs of new value and expected version
   * @return Map of keys to success status for each operation
   */
  suspend fun putWithVersions(updates: Map<String, Pair<ByteArray?, Long>>): Map<String, Boolean>

  @TestOnly
  fun flush()
}
