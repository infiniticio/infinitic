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

package io.infinitic.autoclose

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Each class in Infinitic is in charge of closing the resources it creates.
 * But sometimes, a resource is created by a class and used by another receiver one.
 * We use this to keep track of all resources needing to be closed when the receiver class is closed.
 */
object AutoClose {
  // Global thread safe map of set of closeable resources
  private val resources = ConcurrentHashMap<AutoCloseable, CopyOnWriteArraySet<AutoCloseable>>()

  fun add(obj: AutoCloseable, resource: AutoCloseable) {
    resources.getOrPut(obj) { CopyOnWriteArraySet() }.add(resource)
  }

  fun close(obj: AutoCloseable) {
    resources[obj]?.forEach { resource -> resource.close() }
    resources.remove(obj)
  }
}

/**
 * Add a resource to close when the receiver object is closed
 */
fun AutoCloseable.addAutoCloseResource(resource: AutoCloseable) =
    AutoClose.add(this, resource)

/**
 * Close resources added to the receiver object
 * This method must be called from the close() method of the receiver object
 */
fun AutoCloseable.autoClose() = AutoClose.close(this)
