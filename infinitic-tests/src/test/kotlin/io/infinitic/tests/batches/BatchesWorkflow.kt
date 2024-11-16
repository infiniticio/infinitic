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
import io.infinitic.workflows.and
import kotlin.random.Random

@Name("batchWorkflow")
internal interface BatchWorkflow {
  fun foo(value: Int): Int
  fun foo2(foo: Int, bar: Int): Int
  fun foo4(foo: Int, bar: Int): Input
  fun foo5(foo: Int, bar: Int): Input
  fun foo6(foo: Int, bar: Int)
  fun withKey(n: Int): Boolean
  fun withDelay(delay: Long)
}

@Suppress("unused")
internal class BatchWorkflowImpl : Workflow(), BatchWorkflow {

  private val batchService = newService(BatchService::class.java)
  private val metaFoo: Map<String, ByteArray> = mapOf("batchKey" to "Foo".toByteArray())
  private val metaBar: Map<String, ByteArray> = mapOf("batchKey" to "Bar".toByteArray())
  private val batchServiceWithKeyFoo = newService(BatchService::class.java, null, metaFoo)
  private val batchServiceWithKeyBar = newService(BatchService::class.java, null, metaBar)

  override fun foo(value: Int) = batchService.foo(value)
  override fun foo2(foo: Int, bar: Int) = batchService.foo2(foo, bar)
  override fun foo4(foo: Int, bar: Int) = batchService.foo4(foo)
  override fun foo5(foo: Int, bar: Int) = batchService.foo5(Input(foo, bar))
  override fun foo6(foo: Int, bar: Int) = batchService.foo6(Input(foo, bar))
  override fun withKey(n: Int): Boolean {
    val deferredList = List(n) {
      when (inline { Random.nextBoolean() }) {
        true -> dispatch(batchServiceWithKeyFoo::haveSameKey, it)
        false -> dispatch(batchServiceWithKeyBar::haveSameKey, it)
      }
    }
    // return true if all true
    return deferredList.and().await().all { it }
  }

  override fun withDelay(delay: Long) {
    inline { Thread.sleep(delay) }
  }
}
