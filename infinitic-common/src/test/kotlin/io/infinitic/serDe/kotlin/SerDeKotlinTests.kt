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

package io.infinitic.serDe.kotlin

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.workflows.data.steps.Step
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Duration
import java.time.Instant
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

class SerDeTests : StringSpec(
    {

      "Primitive (ByteArray) should be serializable / deserializable (with type)" {
        val bytes1: ByteArray = TestFactory.random()
        val bytes2 = SerializedData.encode(bytes1, type = bytes1::class.java, null)
            .decode(bytes1::class.java, null) as ByteArray

        bytes1.contentEquals(bytes2) shouldBe true
      }

      "Primitive (ByteArray) should be serializable / deserializable (without type)" {
        val bytes1: ByteArray = TestFactory.random()
        val bytes2 = SerializedData.encode(bytes1, null, null)
            .decode(null, null) as ByteArray

        bytes1.contentEquals(bytes2) shouldBe true
      }

      "Primitive (Short) should be serializable / deserializable (with type)" {
        val val1: Short = TestFactory.random()
        val val2 = SerializedData.encode(val1, Short::class.java, null)
            .decode(Short::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Short) should be serializable / deserializable (without type)" {
        val val1: Short = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Primitive (Int) should be serializable / deserializable (with type)" {
        val val1: Int = TestFactory.random()
        val val2 = SerializedData.encode(val1, Int::class.java, null)
            .decode(Int::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Int) should be serializable / deserializable (without type)" {
        val val1: Int = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Primitive (Long) should be serializable / deserializable (with type)" {
        val val1: Long = TestFactory.random()
        val val2 = SerializedData.encode(val1, Long::class.java, null)
            .decode(Long::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Long) should be serializable / deserializable (without type)" {
        val val1: Long = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Primitive (Float) should be serializable / deserializable (with type)" {
        val val1: Float = TestFactory.random()
        val val2 = SerializedData.encode(val1, Float::class.java, null)
            .decode(Float::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Float) should be serializable / deserializable (without type)" {
        val val1: Float = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Primitive (Double) should be serializable / deserializable (with type)" {
        val val1: Double = TestFactory.random()
        val val2 = SerializedData.encode(val1, Double::class.java, null)
            .decode(Double::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Double) should be serializable / deserializable (without type)" {
        val val1: Double = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Primitive (Boolean) should be serializable / deserializable (with type)" {
        val val1: Boolean = TestFactory.random()
        val val2 = SerializedData.encode(val1, Boolean::class.java, null)
            .decode(Boolean::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Boolean) should be serializable / deserializable (without type)" {
        val val1: Boolean = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Primitive (Char) should be serializable / deserializable (with type)" {
        val val1: Char = TestFactory.random()
        val val2 = SerializedData.encode(val1, Char::class.java, null)
            .decode(Char::class.java, null)

        val2 shouldBe val1
      }

      "Primitive (Char) should be serializable / deserializable (without type)" {
        val val1: Char = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "String should be serializable / deserializable (with type)" {
        val val1: String = TestFactory.random()
        val val2 = SerializedData.encode(val1, String::class.java, null)
            .decode(String::class.java, null)

        val2 shouldBe val1
      }

      "Instant should be serializable / deserializable (with type)" {
        val val1: Instant = Instant.now()
        val val2 = SerializedData.encode(val1, Instant::class.java, null)
            .also { println(it); println(Instant::class.java) }
            .decode(Instant::class.java, null)

        val2 shouldBe val1
      }

      "Instant should be serializable / deserializable (without type)" {
        val val1: Instant = Instant.now()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "Duration should be serializable / deserializable (with type)" {
        val val1: Duration = Duration.ofMillis(42)
        val val2 = SerializedData.encode(val1, Duration::class.java, null)
            .decode(Duration::class.java, null)

        val2 shouldBe val1
      }

      "Duration should be serializable / deserializable (without type)" {
        val val1: Duration = Duration.ofMillis(42)
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

      "String should be serializable / deserializable (without type)" {
        val val1: String = TestFactory.random()
        val val2 = SerializedData.encode(val1, null, null)
            .decode(null, null)

        val2 shouldBe val1
      }

//      "List Of primitives should be serializable / deserializable" {
//        val val1 = listOf(42F, true, "!@#%")
//
//        val val2 = SerializedData.encode(val1, null, null)
//            .decode(null, null)
//
//        val2 shouldBe val1
//      }

      "Null (Obj) should be serializable / deserializable" {
        val obj1: Obj1? = null
        val obj2 = SerializedData.encode(obj1, Obj1::class.java, null)
            .decode(Obj1::class.java, null)

        obj2 shouldBe obj1
      }

      "Simple Object should be serializable / deserializable" {
        val val1 = Obj1("42", 42, Type.TYPE_1)
        val val2 = SerializedData.encode(val1, Obj1::class.java, null)
            .decode(Obj1::class.java, null)

        val2 shouldBe val1
      }

      "Object should be deserializable even with more properties" {
        val val1 = Obj1("42", 42, Type.TYPE_1)
        val data = SerializedData.encode(val1, Obj1::class.java, null)
        val val2 = data.decode(Obj2::class.java, null) as Obj2

        val2.foo shouldBe val1.foo
        val2.bar shouldBe val2.bar
      }

      "Object should be deserializable even with less properties, using default values" {
        val val1 = Obj2("42", 42)
        val data = SerializedData.encode(val1, Obj2::class.java, null)
        val val2 = data.decode(Obj1::class.java, null) as Obj1

        val2.foo shouldBe val1.foo
        val2.bar shouldBe val1.bar
        val2.type shouldBe Type.TYPE_1
      }

      "Object containing set of serializable should be serializable / deserializable" {
        val obj = Obj1("42", 42, Type.TYPE_2)
        val val1 = SetOfObj1s(setOf(obj))
        val data = SerializedData.encode(val1, SetOfObj1s::class.java, null)
        val val2 = data.decode(SetOfObj1s::class.java, null)

        val2 shouldBe val1
      }

      "Object containing set of sealed serializable should be serializable / deserializable" {
        val obj = Obj1("42", 42, Type.TYPE_2)
        val val1 = SetOfObjs(setOf(obj))
        val data = SerializedData.encode(val1, SetOfObjs::class.java, null)
        val val2 = data.decode(SetOfObjs::class.java, null)

        val2 shouldBe val1
      }

      "Object containing set of sealed interfaces should be serializable / deserializable" {
        json = Json {
          classDiscriminator = "klass"
          serializersModule = SerializersModule {
            polymorphic(WithFoo::class, Obj1::class, Obj1.serializer())
            polymorphic(WithFoo::class, Obj2::class, Obj2.serializer())
          }
        }
        val obj = Obj1("42", 42, Type.TYPE_2)
        val val1 = SetOfWithFoo(setOf(obj))
        val data = SerializedData.encode(val1, SetOfWithFoo::class.java, null)
        val val2 = data.decode(SetOfWithFoo::class.java, null)

        val2 shouldBe val1
      }

      "Step.Id should be serializable / deserializable" {
        val step1 = TestFactory.random<Step.Id>()
        val step2 = SerializedData.encode(step1, Step::class.java, null)
            .decode(Step::class.java, null)

        step2 shouldBe step1
      }

      "Step.Or should be serializable / deserializable" {
        val step1 = TestFactory.random<Step.Or>()
        val step2 = SerializedData.encode(step1, Step::class.java, null)
            .decode(Step::class.java, null)

        step2 shouldBe step1
      }

      "Step.And should be serializable / deserializable" {
        val step1 = TestFactory.random<Step.And>()
        val step2 = SerializedData.encode(step1, Step::class.java, null)
            .decode(Step::class.java, null)

        step2 shouldBe step1
      }

      "Comparing SerializedData" {
        val step = TestFactory.random<Step>()
        val s1 = SerializedData.encode(step, Step::class.java, null)
        val s2 = SerializedData.encode(step, Step::class.java, null)

        s1 shouldBe s2
      }

      "List of Objects wit serializer should be serializable / deserializable (without type)" {
        val val1 = listOf(Obj1("42", 42, Type.TYPE_1), Obj1("24", 24, Type.TYPE_2))
        val ser = SerializedData.encode(val1, null, null)
        println(ser)
        val val2 = ser.decode(null, null)

        val2 shouldBe val1
      }

      "List of  Objects wit serializer should be serializable / deserializable (with type)" {
        val val1 = listOf(Obj1("42", 42, Type.TYPE_1), Obj1("24", 24, Type.TYPE_2))
        val ser = SerializedData.encode(val1, typeOf<List<Obj1>>().javaType, null)
        println(ser)
        val val2 = ser.decode(typeOf<List<Obj1>>().javaType, null)

        val2 shouldBe val1
      }
    },
)


@Serializable
private sealed class Obj

private enum class Type {
  TYPE_1,
  TYPE_2
}

@Serializable
@SerialName("Obj1")
private data class Obj1(
  override val foo: String,
  val bar: Int,
  val type: Type = Type.TYPE_1
) : Obj(), WithFoo {

  fun test(pojo: List<Map<Int, WithFoo>>) {
    println("from test(): $pojo")
  }

  fun test2(pojo: Obj) {
    println("from test2(): $pojo")
  }
}

@Serializable
@SerialName("Obj2")
private data class Obj2(override val foo: String, val bar: Int) : Obj(), WithFoo

@Serializable
private data class SetOfObj1s(val objs: Set<Obj1>)

@Serializable
private data class SetOfObjs(val objs: Set<Obj>)

@Serializable
private data class SetOfWithFoo(val objs: Set<WithFoo>)

@Polymorphic
private sealed interface WithFoo {
  val foo: String
}

