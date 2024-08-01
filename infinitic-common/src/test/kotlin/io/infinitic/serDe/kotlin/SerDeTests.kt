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
import kotlinx.serialization.serializer
import java.lang.reflect.Method

class SerDeTests : StringSpec(
    {
      "Primitive (ByteArray) should be serializable / deserializable" {
        val bytes1: ByteArray = TestFactory.random()
        val bytes2 = SerializedData.from(bytes1).deserialize(null) as ByteArray

        bytes1.contentEquals(bytes2) shouldBe true
      }

      "Primitive (Short) should be serializable / deserializable" {
        val val1: Short = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Primitive (Int) should be serializable / deserializable" {
        val val1: Int = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Primitive (Long) should be serializable / deserializable" {
        val val1: Long = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Primitive (Float) should be serializable / deserializable" {
        val val1: Float = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Primitive (Double) should be serializable / deserializable" {
        val val1: Double = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Primitive (Boolean) should be serializable / deserializable" {
        val val1: Boolean = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Primitive (Char) should be serializable / deserializable" {
        val val1: Char = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "String should be serializable / deserializable" {
        val val1: String = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "List Of primitives should be serializable / deserializable" {
        val val1 = listOf(42F, true, "!@#%")
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Null (Obj) should be serializable / deserializable" {
        val obj1: Obj1? = null
        val obj2 = SerializedData.from(obj1).deserialize(null)

        obj2 shouldBe obj1
      }

      "Simple Object should be serializable / deserializable" {
        val val1 = Obj1("42", 42, Type.TYPE_1)
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Object should be deserializable with more properties" {
        val val1 = Obj1("42", 42, Type.TYPE_1)
        val data = SerializedData.from(val1).copy(
            meta = mapOf(
                SerializedData.META_JAVA_CLASS to
                    Obj2::class.java.name.toByteArray(charset = Charsets.UTF_8),
            ),
        )
        val val2 = data.deserialize(null) as Obj2

        val2.foo shouldBe val1.foo
        val2.bar shouldBe val2.bar
      }

      "Object should be deserializable with less properties using default values" {
        val val1 = Obj2("42", 42)
        val data = SerializedData.from(val1).copy(
            meta = mapOf(
                SerializedData.META_JAVA_CLASS to
                    Obj1::class.java.name.toByteArray(charset = Charsets.UTF_8),
            ),
        )
        val val2 = data.deserialize(null) as Obj1

        val2.foo shouldBe val1.foo
        val2.bar shouldBe val1.bar
        val2.type shouldBe Type.TYPE_1
      }

      "Object containing set of sealed should be serializable / deserializable" {
        json = Json {
          classDiscriminator = "klass"
          serializersModule = SerializersModule {
            polymorphic(WithFoo::class, Obj1::class, Obj1.serializer())
            polymorphic(WithFoo::class, Obj2::class, Obj2.serializer())
          }
        }
        val obj = Obj1("42", 42, Type.TYPE_2)
        val val1 = Objs(setOf(obj), setOf(obj))
        val val2 = SerializedData.from(val1).deserialize(null)

        val2 shouldBe val1
      }

      "Step.Id should be serializable / deserializable" {
        val step1 = TestFactory.random<Step.Id>()
        val step2 = SerializedData.from(step1).deserialize(null)

        step2 shouldBe step1
      }

      "Step.Or should be serializable / deserializable" {
        val step1 = TestFactory.random<Step.Or>()
        val step2 = SerializedData.from(step1).deserialize(null)

        step2 shouldBe step1
      }

      "Step.And should be serializable / deserializable" {
        val step1 = TestFactory.random<Step.And>()
        val step2 = SerializedData.from(step1).deserialize(null)

        step2 shouldBe step1
      }

      "Comparing SerializedData" {
        val step = TestFactory.random<Step>()
        val s1 = SerializedData.from(step)
        val s2 = SerializedData.from(step)

        s1 shouldBe s2
      }

//      "List of simple Objects should be serializable / deserializable" {
//        val val1 = listOf(Obj1("42", 42, Type.TYPE_1), Obj1("24", 24, Type.TYPE_2))
//        val ser = SerializedData.from(val1)
//        println(ser.toJsonString())
//        val val2 = ser.deserialize(null)
//
//        val2 shouldBe val1
//      }
    },
)

fun main() {
  val json = Json {
    classDiscriminator = "#klass"

    serializersModule = SerializersModule {
      polymorphic(WithFoo::class, Obj1::class, Obj1.serializer())
      polymorphic(WithFoo::class, Obj2::class, Obj2.serializer())
    }
  }

  val method: Method = Obj1::class.java.getMethod("test", List::class.java)
  val type = method.parameters[0].parameterizedType
  val serializer = serializer(type)

  val o: List<Map<Int, WithFoo>> = listOf(mapOf(1 to Obj1("a", 2, Type.TYPE_1)))

  val oStr = json.encodeToString(serializer, o)

  println("oStr = $oStr")

  val o2: Any = json.decodeFromString(serializer, oStr)

  println("o2 = $o2")
  println("o = o2 : ${o == o2}")
}

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
private data class Objs(val objs: Set<Obj>, val others: Set<WithFoo>)

@Polymorphic
private sealed interface WithFoo {
  val foo: String
}
