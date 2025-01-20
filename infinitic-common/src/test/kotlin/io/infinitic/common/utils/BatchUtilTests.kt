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

package io.infinitic.common.utils

import io.infinitic.annotations.Batch
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BatchUtilTests : StringSpec(
    {
      "Find single for batch method with 1 parameter" {
        val klass = FooBatch1::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Int::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with 2 parameters" {
        val klass = FooBatch2::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Int::class.java, Int::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with 1 collection parameters" {
        val klass = FooBatch3::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Set::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with no return, batch with List<unit>" {
        val klass = FooBatch4::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Int::class.java, Int::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with no return" {
        val klass = FooBatch5::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Int::class.java, Int::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with generic parameters" {
        val klass = FooBatch6::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", MyPair::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with Parent return type" {
        val klass = FooBatch7::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Int::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "Find single for batch method with Nothing return type" {
        val klass = FooBatch8::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        val single = klass.getMethod("bar", Int::class.java)
        val batch = klass.getMethod("bar", Map::class.java)
        list.find { it.single == single }?.batch shouldBe batch
      }

      "batch method with vararg should throw" {
        val klass = FooBatchError0::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("exactly one parameter")
      }

      "batch method with more than 1 parameter should throw" {
        val klass = FooBatchError1::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("exactly one parameter")
      }

      "batch method without corresponding single method should throw" {
        val klass = FooBatchError2::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("No single method found")
      }

      "batch method with the wrong return type should throw" {
        val klass = FooBatchError4::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("return type")
      }

      "batch method with a return type other than List should throw" {
        val klass = FooBatchError5::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("return type")
      }

      "batch method with a map not based on string should throw" {
        val klass = FooBatchError6::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("Map<String, T>")
      }
    },
)

fun main() {
  val klass = FooBatch1::class.java
  klass.getBatchMethods().forEach { println(it) }
  klass.methods.filter { it.name == "bar" }[0].let {
    println(it)
  }
}

// 1 parameter - Batched method with Collection parameter
private class FooBatch1 {
  fun bar(p: Int): String = p.toString()

  @Batch
  fun bar(p: Map<String, Int>): Map<String, String> =
      p.mapValues { it.value.toString() }
}

// 2 parameters - Batched method with Collection parameter
private class FooBatch2 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(l: Map<String, PairInt>): Map<String, String> =
      l.mapValues { bar(it.value.p, it.value.q) }
}

// 1 parameter - Batched method with Collection parameter
private class FooBatch3 {
  fun bar(p: Set<Int>): String = p.toString()

  @Batch
  fun bar(p: Map<String, Set<Int>>): Map<String, String> =
      p.mapValues { it.value.toString() }
}

// 1 parameter - No return - return List<Unit>
private class FooBatch4 {
  fun bar(p: Int, q: Int) {
    // do nothing
  }

  @Batch
  fun bar(p: Map<String, PairInt>): Map<String, Unit> =
      p.mapValues { it.value.toString() }
}

// 1 parameter - No return
private class FooBatch5 {
  fun bar(p: Int, q: Int) {
    // do nothing
  }

  @Batch
  fun bar(p: Map<String, PairInt>) {
    // do nothing
  }
}

// 1 parameter - Parameter with Generic
private class FooBatch6 {
  fun bar(pair: MyPair<Int>) {
    // do nothing
  }

  @Batch
  fun bar(pairs: Map<String, MyPair<Int>>) {
    // do nothing
  }
}

// Single method with parent return value
private class FooBatch7 : FooBatch {
  override fun bar(p: Int) = PPairInt(p, p)

  @Batch
  fun bar(list: Map<String, Int>): Map<String, PairInt> =
      list.mapValues { PairInt(it.value, it.value) }
}

internal interface FooBatch {
  fun bar(p: Int): PairInt
}

// Single method Nothing return value
private class FooBatch8 : FooBatch {
  override fun bar(p: Int): Nothing = thisShouldNotHappen()

  @Batch
  fun bar(list: Map<String, Int>): Map<String, PairInt> =
      list.mapValues { PairInt(it.value, it.value) }
}

internal class PPairInt(override val p: Int, override val q: Int) : PairInt(p, q)

// vararg not accepted
private class FooBatchError0 {
  fun bar(p: Int): String = p.toString()

  @Batch
  fun bar(vararg p: Int): List<String> = p.map { it.toString() }
}

// annotation @Batch without corresponding single method with the right parameters
private class FooBatchError1 {
  fun bar(p: Int): String = p.toString()

  @Batch
  fun bar(p: Map<String, Int>, q: Int): Map<String, String> = p.mapValues { it.toString() }
}

// annotation @Batch without corresponding single method with the right parameters
private class FooBatchError2 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(p: Map<String, Int>): Map<String, String> = p.mapValues { it.toString() }
}

// Not the right return type
private class FooBatchError4 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(p: Map<String, PairInt>): Map<String, Int> = p.mapValues { it.value.p + it.value.q }
}

// Not a Map in return type
private class FooBatchError5 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(p: Map<String, PairInt>): List<String> = listOf("?")
}

private class FooBatchError6 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch
  fun bar(p: Map<Int, PairInt>): Map<String, String> = mapOf("?" to "?")
}

internal open class PairInt(open val p: Int, open val q: Int)

private class MyPair<T>(val p: T, val q: T)

