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
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BatchUtilTests : StringSpec(
    {
      "Find single for batch method with 1 parameter and List" {
        val klass = FooBatch1::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Int::class.java)
        list[0].batch shouldBe klass.getMethod("bar", List::class.java)
      }

      "Find single for batch method with 1 parameter and vararg" {
        val klass = FooBatch1bis::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Int::class.java)
        list[0].batch shouldBe klass.methods[1]
      }

      "Find single for batch method with 2 parameters and List" {
        val klass = FooBatch2::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Int::class.java, Int::class.java)
        list[0].batch shouldBe klass.getMethod("bar", List::class.java)
      }

      "Find single for batch method with 2 parameters and vararg" {
        val klass = FooBatch2bis::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Int::class.java, Int::class.java)
        list[0].batch shouldBe klass.methods.first { it.name == "bar" && it.parameters.first().isVarArgs }
      }

      "Find single for batch method with 1 collection parameters" {
        val klass = FooBatch3::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Set::class.java)
        list[0].batch shouldBe klass.getMethod("bar", List::class.java)
      }

      "Find single for batch method with no return, batch with List<unit>" {
        val klass = FooBatch4::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Int::class.java, Int::class.java)
        list[0].batch shouldBe klass.getMethod("bar", List::class.java)
      }

      "Find single for batch method with no return" {
        val klass = FooBatch5::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", Int::class.java, Int::class.java)
        list[0].batch shouldBe klass.getMethod("bar", List::class.java)
      }

      "Find single for batch method with generic parameters" {
        val klass = FooBatch6::class.java
        val list = shouldNotThrowAny { klass.getBatchMethods() }
        list.size shouldBe 1
        list[0].single shouldBe klass.getMethod("bar", MyPair::class.java)
        list[0].batch shouldBe klass.getMethod("bar", List::class.java)
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

      "multiple batch methods for the same single method should throw" {
        val klass = FooBatchError3::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("Multiple @Batch methods")
      }

      "batch method with the wrong return type should throw" {
        val klass = FooBatchError4::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("No single method found")
      }

      "batch method with a return type other than List should throw" {
        val klass = FooBatchError5::class.java
        val e = shouldThrowAny { klass.getBatchMethods() }
        e.message.shouldContain("return type")
      }
    },
)

// 1 parameter - Batched method with Collection parameter
private class FooBatch1 {
  fun bar(p: Int): String = p.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<Int>): List<String> = p.map { it.toString() }
}

// 1 parameter - Batched method with vararg parameters
private class FooBatch1bis {
  fun bar(p: Int): String = p.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(vararg p: Int): List<String> = p.map { it.toString() }
}

// 2 parameters - Batched method with Collection parameter
private class FooBatch2 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(l: List<PairInt>): List<String> = l.map { bar(it.p, it.q) }
}

// 2 parameters - Batched method with vararg parameter
private class FooBatch2bis {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(vararg l: PairInt): List<String> = l.map { bar(it.p, it.q) }
}

// 1 parameter - Batched method with Collection parameter
private class FooBatch3 {
  fun bar(p: Set<Int>): String = p.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<Set<Int>>): List<String> = p.map { it.toString() }
}

// 1 parameter - No return - return List<Unit>
private class FooBatch4 {
  fun bar(p: Int, q: Int) {
    // do nothing
  }

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<PairInt>): List<Unit> = p.map { it.toString() }
}

// 1 parameter - No return
private class FooBatch5 {
  fun bar(p: Int, q: Int) {
    // do nothing
  }

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<PairInt>) {
    // do nothing
  }
}

// 1 parameter - Parameter with Generic
private class FooBatch6 {
  fun bar(pair: MyPair<Int>) {
    // do nothing
  }

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(pairs: List<MyPair<Int>>) {
    // do nothing
  }
}

// annotation @Batch without corresponding single method with the right parameters
private class FooBatchError1 {
  fun bar(p: Int): String = p.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<Int>, q: Int): List<String> = p.map { it.toString() }
}

// annotation @Batch without corresponding single method with the right parameters
private class FooBatchError2 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<Int>): List<String> = p.map { it.toString() }
}

// double annotation @Batch for the same single method
private class FooBatchError3 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<PairInt>): List<String> = p.map { it.toString() }

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(vararg p: PairInt): List<String> = p.map { it.toString() }
}

// Not the right return type
private class FooBatchError4 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<PairInt>): List<Int> = p.map { it.p + it.q }
}

// Not a List in return type
private class FooBatchError5 {
  fun bar(p: Int, q: Int): String = p.toString() + q.toString()

  @Batch(maxMessage = 10, maxDelaySeconds = 1.0)
  fun bar(p: List<PairInt>): String = "?"
}

private class PairInt(val p: Int, val q: Int)

private class MyPair<T>(val p: T, val q: T)

