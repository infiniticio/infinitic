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

import io.infinitic.common.fixtures.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SerDeTests : StringSpec({
    "Primitive (ByteArray) should be serializable / deserializable" {
        val bytes1: ByteArray = TestFactory.random<String>().toByteArray()
        val bytes2 = SerializedData.from(bytes1).deserialize() as ByteArray

        bytes1.contentEquals(bytes2) shouldBe true
    }

    "Primitive (Short) should be serializable / deserializable" {
        val val1: Short = 42
        val val2 = SerializedData.from(val1).deserialize() as Short

        val1 shouldBe val2
    }

    "Primitive (Int) should be serializable / deserializable" {
        val val1: Int = 42
        val val2 = SerializedData.from(val1).deserialize() as Int

        val1 shouldBe val2
    }

    "Primitive (Long) should be serializable / deserializable" {
        val val1: Long = 42
        val val2 = SerializedData.from(val1).deserialize() as Long

        val1 shouldBe val2
    }

    "Primitive (Float) should be serializable / deserializable" {
        val val1: Float = 42.0F
        val val2 = SerializedData.from(val1).deserialize() as Float

        val1 shouldBe val2
    }

    "Primitive (Double) should be serializable / deserializable" {
        val val1: Double = 42.0
        val val2 = SerializedData.from(val1).deserialize() as Double

        val1 shouldBe val2
    }

    "Primitive (Boolean) should be serializable / deserializable" {
        val val1: Boolean = true
        val val2 = SerializedData.from(val1).deserialize() as Boolean

        val1 shouldBe val2
    }

    "Primitive (Char) should be serializable / deserializable" {
        val val1: Char = 'w'
        val val2 = SerializedData.from(val1).deserialize() as Char

        val1 shouldBe val2
    }

    "String should be serializable / deserializable" {
        val val1: String = TestFactory.random()
        val val2 = SerializedData.from(val1).deserialize() as String

        val1 shouldBe val2
    }

    "List Of primitives should be serializable / deserializable" {
        val val1 = listOf(42F, true, "!@#%")
        val val2 = SerializedData.from(val1).deserialize() as List<*>

        val1 shouldBe val2
    }

    "Simple Object should be serializable / deserializable" {
        val val1 = Obj1("42", 42)
        val val2 = SerializedData.from(val1).deserialize() as Obj1

        val1 shouldBe val2
    }

//    "List of simple Objects should be serializable / deserializable" {
//        val val1 = listOf(Obj1("42", 42), Obj1("24", 24))
//        val val2 = SerializedData.from(val1).deserialize() as List<Obj1>
//
//        val1 shouldBe val2
//    }
})

sealed class Obj

data class Obj1(val foo: String, val bar: Int) : Obj()

data class Obj2(val foo: String, val bar: Int) : Obj()

data class Obj3(val foo: String) : Obj()
