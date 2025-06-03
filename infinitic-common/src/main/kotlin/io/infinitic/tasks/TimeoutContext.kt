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

package io.infinitic.tasks

import io.infinitic.common.exceptions.thisShouldNotHappen
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Context for managing task timeouts and cancellation.
 *
 * This class provides a mechanism to:
 * - Register a callback to be invoked when a timeout occurs.
 * - Track whether the timeout has been triggered or cancelled.
 * - Store the thread currently executing the task.
 * - Provide thread-local access to the current timeout context.
 *
 * Usage:
 * - Use [onTimeout] to register a callback to be executed on timeout.
 * - Call [onTimeOut] to trigger the timeout and execute the callback (if not already cancelled).
 * - Use [isCancelled] to check if the timeout has already been triggered.
 * - Use [TimeoutContext.set], [TimeoutContext.current], and [TimeoutContext.clear] to manage the context in thread-local storage.
 */
class TimeoutContext() {
  // Indicates if the timeout has been triggered or cancelled
  internal val _isCancelled = AtomicBoolean(false)

  /** Returns true if the timeout has been triggered or canceled. */
  val isCancelled: Boolean get() = _isCancelled.get()

  /** Callback to be invoked when the timeout occurs. */
  internal var timeoutCallback: Runnable? = null

  /**
   * Triggers the timeout if it hasn't been triggered yet.
   * Invokes the registered [timeoutCallback] if present.
   */
  fun onTimeOut() {
    if (_isCancelled.compareAndSet(false, true)) {
      timeoutCallback?.run()
    }
  }

  /**
   * Registers a callback to be executed when the timeout occurs.
   * @param callback The function to call on timeout.
   */
  fun onTimeout(callback: Runnable) {
    timeoutCallback = callback
  }

  /** The thread currently executing the task (optional, may be null). */
  var executingThread: Thread? = null

  companion object {
    // Thread-local storage for TimeoutContext
    private val threadLocal = ThreadLocal<TimeoutContext>()

    /** Sets the current thread's TimeoutContext. */
    fun set(ctx: TimeoutContext) = threadLocal.set(ctx)

    /**
     * Gets the current thread's TimeoutContext.
     * Throws if not set.
     */
    fun current(): TimeoutContext = threadLocal.get() ?: thisShouldNotHappen()

    /** Clears the current thread's TimeoutContext. */
    fun clear() = threadLocal.remove()
  }
}
