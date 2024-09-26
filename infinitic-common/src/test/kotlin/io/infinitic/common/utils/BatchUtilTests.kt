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
package io.infinitic.common.utils

import io.infinitic.annotations.Batch
import io.kotest.core.spec.style.StringSpec

class BatchUtilTests : StringSpec(
    {
      "getMethodPerNameAndParameters should return method" {

      }
    },
)

fun main() {
  println(FooBatch4::class.java.getBatchMethods())
}

// 1 parameter - Batched method with Collection parameter
private class FooBatch1 {
  fun bar(p: Int): String = p.toString()

  @Batch
  fun bar(p: List<Int>): List<String> = p.map { it.toString() }
}

// 1 parameter - Batched method with vararg parameters
private class FooBatch1bis {
  fun bar(p: Int): String = p.toString()

  @Batch
  fun bar(vararg p: Int): List<String> = p.map { it.toString() }
}

// 2 parameters - Batched method with Collection parameter
private class FooBatch2 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(l: List<PairInt>): List<String> = l.map { bar(it.p, it.q) }
}

// 2 parameters - Batched method with vararg parameter
private class FooBatch2bis {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(vararg l: PairInt): List<String> = l.map { bar(it.p, it.q) }
}

// 1 parameter - Batched method with Collection parameter
private class FooBatch3 {
  fun bar(p: Set<Int>): String = p.toString()

  @Batch
  fun bar(p: List<Set<Int>>): List<String> = p.map { it.toString() }
}

// 1 parameter - Batched method with Collection parameter
private class FooBatch4 {
  fun bar(p: ArrayList<Int>): String = p.toString()

  @Batch
  fun bar(p: List<ArrayList<Int>>): List<String> = p.map { it.toString() }
}

// annotation @Batch without corresponding single method with the right parameters
private class FooBatchError1 {
  fun bar(p: Int): String = p.toString()

  @Batch
  fun bar(p: List<Int>, q: Int): List<String> = p.map { it.toString() }
}

// annotation @Batch without corresponding single method with the right parameters
private class FooBatchError2 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(p: List<Int>): List<String> = p.map { it.toString() }
}

// double annotation @Batch for the same single method
private class FooBatchError3 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(p: List<PairInt>): List<String> = p.map { it.toString() }

  @Batch
  fun bar(vararg p: PairInt): List<String> = p.map { it.toString() }
}

private class PairInt(val p: Int, val q: Int)


