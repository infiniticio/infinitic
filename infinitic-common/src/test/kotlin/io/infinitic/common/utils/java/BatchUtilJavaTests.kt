package io.infinitic.common.utils.java

import io.infinitic.common.utils.getBatchMethods
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BatchUtilJavaTests : StringSpec(
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
