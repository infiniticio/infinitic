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
import io.infinitic.tasks.Task
import io.mockk.InternalPlatformDsl.toArray

fun main() {
  // BatchServiceImpl::class.java.getBatchMethods().forEach { println(it) }
  val l: List<Any> = listOf(Input(1, 2), Input(3, 4))

  val m1 = BatchServiceImpl::class.java.methods.filter { it.name == "foo2" }[2]
  println(m1)
  println(m1.invoke(BatchServiceImpl(), l))

  val m2 = BatchServiceImpl::class.java.methods.filter { it.name == "bar2" }[0]
  println(m2)
  m2.invoke(BatchServiceImpl(), l.toArray())
}

@Name("batchService")
internal interface BatchService {
  fun foo(foo: Int): Int
  fun foo2(foo: Int, bar: Int): Int
  fun foo3(input: Input): Int
  fun foo4(foo: Int): Input
  fun foo5(input: Input): Input
  fun haveSameKey(i: Int): Boolean
}

internal class BatchServiceImpl : BatchService {

  override fun foo(foo: Int) = thisShouldNotHappen()
  override fun foo2(foo: Int, bar: Int) = thisShouldNotHappen()
  override fun foo3(input: Input) = thisShouldNotHappen()
  override fun foo4(foo: Int) = thisShouldNotHappen()
  override fun foo5(input: Input) = thisShouldNotHappen()
  override fun haveSameKey(i: Int) = thisShouldNotHappen()

  @Batch(maxMessages = 10, maxSeconds = 1.0)
  fun foo(list: Map<String, Int>): Map<String, Int> =
      list.mapValues { list.values.sum() }

  @Batch(maxMessages = 10, maxSeconds = 1.0)
  fun foo2(list: Map<String, Input>): Map<String, Int> =
      list.mapValues { list.values.sumOf { it.sum() } }

  @Batch(maxMessages = 10, maxSeconds = 1.0)
  fun foo3(list: Map<String, Input>): Map<String, Int> =
      list.mapValues { list.values.sumOf { it.sum() } }

  @Batch(maxMessages = 10, maxSeconds = 1.0)
  fun foo4(list: Map<String, Int>): Map<String, Input> =
      list.mapValues { Input(list.values.sumOf { it }, it.value) }

  @Batch(maxMessages = 10, maxSeconds = 1.0)
  fun foo5(list: Map<String, Input>): Map<String, Input> =
      list.mapValues { Input(list.values.sumOf { it.sum() }, it.value.bar) }

  @Batch(maxMessages = 10, maxSeconds = 2.0)
  fun haveSameKey(all: Map<String, Int>): Map<String, Boolean> {
    // get batch key for the first element
    val batchKeys = all.keys.map { Task.getContext(it)!!.batchKey }
    println("batchKeys = $batchKeys")
    println("all = $all")
    val batchKey = batchKeys.first()
    // if all have the same batch keys then this should return Map<String, true>
    val allHaveSameKey = batchKeys.all { it == batchKey }
    return all.mapValues { allHaveSameKey }
  }
}

internal data class Input(val foo: Int, val bar: Int) {
  fun sum() = foo + bar
}
