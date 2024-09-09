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
package io.infinitic.workers.scafold

import io.infinitic.common.fixtures.later
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private class WorkerScaffold {
  private val scope = CoroutineScope(Dispatchers.IO)

  fun startAsync() = scope.future {
    launch {
      while (isActive) {
        delay(100)
      }
    }
    launch {
      while (isActive) {
        delay(100)
        //if (Random.nextDouble() < 0.05) throw RuntimeException("Internal startAsync error")
      }
    }
  }

  fun start() {
    try {
      startAsync().join()
    } catch (e: CancellationException) {
      // do nothing
    } catch (e: Exception) {
      println("closing...")
      close()
      throw e
    }
  }

  fun close() {
    scope.cancel()
  }
}

fun main() {
  val worker = WorkerScaffold()
  later(2000) {
    worker.close()
  }
  worker.start()
  println("ended")
}
