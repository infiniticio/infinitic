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
@file:Suppress("unused")

package io.infinitic.tests.batches

import io.infinitic.annotations.Batch
import io.infinitic.annotations.Name
import io.infinitic.common.exceptions.thisShouldNotHappen

@Name("batchService")
interface BatchService {
  fun add(value: Int): Int
  fun add2(foo: Int, bar: Int): Int
}

internal class BatchServiceImpl : BatchService {

  override fun add(value: Int) = thisShouldNotHappen()

  override fun add2(foo: Int, bar: Int) = thisShouldNotHappen()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun add(list: List<Int>) = List(list.size) { list.sum() }

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun add2(list: List<Input>) = List(list.size) { list.sumOf { it.sum() } }
}

internal data class Input(val foo: Int, val bar: Int) {
  fun sum() = foo + bar
}
