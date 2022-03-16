/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.serDe

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.data.steps.Step
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SerDeJacksonTests : StringSpec({

    "Null (Obj) should be serializable / deserializable" {
        val obj1: JObj1? = null
        val obj2 = SerializedData.from(obj1).deserialize()

        obj2 shouldBe obj1
    }

    "Primitive (ByteArray) should be serializable / deserializable" {
        val bytes1: ByteArray = TestFactory.random()
        val bytes2 = SerializedData.from(bytes1).deserialize() as ByteArray

        bytes1.contentEquals(bytes2) shouldBe true
    }

    "Primitive (Short) should be serializable / deserializable" {
        val val1: Short = TestFactory.random()
        println(SerializedData.from(val1))
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

    "Simple Object should be serializable / deserializable" {
        val val1 = JObj1("42", 42, TypeJ.TYPE_1)
        val val2 = SerializedData.from(val1).deserialize()

        val2 shouldBe val1
    }

    "Object should be deserializable with additionnal properties" {
        val val1 = JObj1("42", 42, TypeJ.TYPE_1)
        val valtmp = JObj2("40", 40)
        val data = SerializedData.from(valtmp).also {
            it.bytes = SerializedData.from(val1).bytes
        }
        val val2 = data.deserialize() as JObj2

        val2.foo shouldBe val1.foo
        val2.bar shouldBe val2.bar
    }

    "Object containing set of sealed should be serializable / deserializable (even with a 'type' property)" {
        val val1 = JObjs(setOf(JObj1("42", 42, TypeJ.TYPE_1)))
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
//        val val1 = listOf(JObj1("42", 42, TypeJ.TYPE_1), JObj1("24", 24, TypeJ.TYPE_2))
//        val val2 = SerializedData.from(val1).deserialize()
//
//        val1 shouldBe val2
//    }
})

@JsonTypeInfo(
    use = JsonTypeInfo.Id.DEDUCTION
)
sealed class JObj

enum class TypeJ {
    TYPE_1,
    TYPE_2,
}

data class JObj1(val foo: String, val bar: Int, val type: TypeJ) : JObj()

data class JObj2(val foo: String, val bar: Int) : JObj()

data class JObjs(val objs: Set<JObj>)
