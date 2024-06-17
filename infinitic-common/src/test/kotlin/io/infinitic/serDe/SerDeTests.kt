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
package io.infinitic.serDe

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.serDe.SerializedData.Companion.META_JAVA_CLASS
import io.infinitic.common.workflows.data.steps.Step
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

class SerDeTests :
  StringSpec(
      {
        "Primitive (ByteArray) should be serializable / deserializable" {
          val bytes1: ByteArray = TestFactory.random()
          val bytes2 = SerializedData.from(bytes1).deserialize() as ByteArray

          bytes1.contentEquals(bytes2) shouldBe true
        }

        "Primitive (Short) should be serializable / deserializable" {
          val val1: Short = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Primitive (Int) should be serializable / deserializable" {
          val val1: Int = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Primitive (Long) should be serializable / deserializable" {
          val val1: Long = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Primitive (Float) should be serializable / deserializable" {
          val val1: Float = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Primitive (Double) should be serializable / deserializable" {
          val val1: Double = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Primitive (Boolean) should be serializable / deserializable" {
          val val1: Boolean = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Primitive (Char) should be serializable / deserializable" {
          val val1: Char = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "String should be serializable / deserializable" {
          val val1: String = TestFactory.random()
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "List Of primitives should be serializable / deserializable" {
          val val1 = listOf(42F, true, "!@#%")
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Null (Obj) should be serializable / deserializable" {
          val obj1: Obj1? = null
          val obj2 = SerializedData.from(obj1).deserialize()

          obj2 shouldBe obj1
        }

        "Simple Object should be serializable / deserializable" {
          val val1 = Obj1("42", 42, Type.TYPE_1)
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Object should be deserializable with more properties" {
          val val1 = Obj1("42", 42, Type.TYPE_1)
          val data =
              SerializedData.from(val1)
                  .copy(
                      meta =
                      mapOf(
                          META_JAVA_CLASS to
                              Obj2::class.java.name.toByteArray(charset = Charsets.UTF_8),
                      ),
                  )
          val val2 = data.deserialize() as Obj2

          val2.foo shouldBe val1.foo
          val2.bar shouldBe val2.bar
        }

        "Object should be deserializable with less properties using default values" {
          val val1 = Obj2("42", 42)
          val data =
              SerializedData.from(val1)
                  .copy(
                      meta =
                      mapOf(
                          META_JAVA_CLASS to
                              Obj1::class.java.name.toByteArray(charset = Charsets.UTF_8),
                      ),
                  )
          val val2 = data.deserialize() as Obj1

          val2.foo shouldBe val1.foo
          val2.bar shouldBe val1.bar
          val2.type shouldBe Type.TYPE_1
        }

        "Object containing set of sealed should be serializable / deserializable (even with a 'type' property)" {
          val val1 = Objs(setOf(Obj1("42", 42, Type.TYPE_1)))
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Step.Id should be serializable / deserializable" {
          val step1 = TestFactory.random<Step.Id>()
          val step2 = SerializedData.from(step1).deserialize()

          step2 shouldBe step1
        }

        "Step.Or should be serializable / deserializable" {
          val step1 = TestFactory.random<Step.Or>()
          val step2 = SerializedData.from(step1).deserialize()

          step2 shouldBe step1
        }

        "Step.And should be serializable / deserializable" {
          val step1 = TestFactory.random<Step.And>()
          val step2 = SerializedData.from(step1).deserialize()

          step2 shouldBe step1
        }

        "Comparing SerializedData" {
          val step = TestFactory.random<Step>()
          val s1 = SerializedData.from(step)
          val s2 = SerializedData.from(step)

          s1 shouldBe s2
        }

        //    "List of simple Objects should be serializable / deserializable" {
        //        val val1 = listOf(Obj1("42", 42, Type.TYPE_1), Obj1("24", 24, Type.TYPE_2))
        //        val val2 = SerializedData.from(val1).deserialize()
        //
        //        val1 shouldBe val2
        //    }
      },
  )

@Serializable
private sealed class Obj

private enum class Type {
  TYPE_1
}

@Serializable
private data class Obj1(val foo: String, val bar: Int, val type: Type = Type.TYPE_1) : Obj()

@Serializable
private data class Obj2(val foo: String, val bar: Int) : Obj()

@Serializable
private data class Objs(val objs: Set<Obj>)
