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
package io.infinitic.common.workflows.engine.storage

/**
 * Optional marker interface for WorkflowStateStorage implementations that support
 * storing convenience fields (status, meta, tags) alongside the workflow state.
 *
 * These convenience fields are extracted from the WorkflowState and stored separately
 * to enable efficient querying without deserializing the full state.
 *
 * Storage backends that don't support this (e.g., Redis) can simply not implement
 * this interface, and the convenience fields will not be stored.
 */
interface WorkflowStateStorageWithConvenience : WorkflowStateStorage {
  /**
   * Indicates whether this storage implementation supports storing convenience fields.
   * This method is provided for runtime detection.
   */
  fun supportsConvenienceFields(): Boolean = true
}
