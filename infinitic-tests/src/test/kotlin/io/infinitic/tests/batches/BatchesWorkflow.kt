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
package io.infinitic.tests.batches

import io.infinitic.annotations.Name
import io.infinitic.workflows.Workflow

@Name("batchWorkflow")
interface BatchWorkflow {
  fun add(value: Int): Int
  fun add2(foo: Int, bar: Int): Int
}

@Suppress("unused")
class BatchWorkflowImpl : Workflow(), BatchWorkflow {

  private val batchService = newService(BatchService::class.java)

  override fun add(value: Int) = batchService.add(value)
  override fun add2(foo: Int, bar: Int) = batchService.add2(foo, bar)
}
