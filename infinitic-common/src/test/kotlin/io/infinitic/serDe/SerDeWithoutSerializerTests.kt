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

import io.infinitic.common.serDe.SerializedData
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

class SerDeWithoutSerializerTests : StringSpec(
    {
      "List of Objects without serializer should be serializable / deserializable (without type)" {
        val val1 = listOf(Pojo1("42", 42, JType.TYPE_1))
        val data = SerializedData.encode(val1, null, null)
        val val2 = data.decode(null, null)

        val2 shouldBe val1
      }

      "List of Objects without serializer should be serializable / deserializable (with type)" {
        val val1 = listOf(Pojo1("42", 42, JType.TYPE_1))
        val data = SerializedData.encode(val1, typeOf<List<Pojo1>>().javaType, null)
        val val2 = data.decode(typeOf<List<Pojo1>>().javaType, null)

        val2 shouldBe val1
      }

      "Array of Objects without serializer should be serializable / deserializable (without type)" {
        val val1 = arrayOf(Pojo1("42", 42, JType.TYPE_1))
        val data = SerializedData.encode(val1, null, null)
        val val2 = data.decode(null, null)

        val2 shouldBe val1
      }

      "Array of Objects without serializer should be serializable / deserializable (with type)" {
        val val1 = arrayOf(Pojo1("42", 42, JType.TYPE_1))
        val data = SerializedData.encode(val1, typeOf<Array<Pojo1>>().javaType, null)
        val val2 = data.decode(typeOf<Array<Pojo1>>().javaType, null)

        val2 shouldBe val1
      }

      "Map of Objects without serializer should be serializable / deserializable (without type)" {
        val val1 = mapOf(1 to Pojo1("42", 42, JType.TYPE_1))
        val data = SerializedData.encode(val1, null, null)
        val val2 = data.decode(null, null)

        val2 shouldBe val1
      }

      "Map of Objects without serializer should be serializable / deserializable (with type)" {
        val val1 = mapOf(1 to Pojo1("42", 42, JType.TYPE_1))
        val data = SerializedData.encode(val1, typeOf<Map<Int, Pojo1>>().javaType, null)
        val val2 = data.decode(typeOf<Map<Int, Pojo1>>().javaType, null)

        val2 shouldBe val1
      }

//      "Array of Map of Objects without serializer should be serializable / deserializable (without type)" {
//        val val1 = arrayOf(
//            mapOf(1 to Pojo1("42", 42, JType.TYPE_1)),
//            mapOf(1 to Pojo1("42", 42, JType.TYPE_1)),
//        )
//        println(val1.inferJavaType())
//        val data = SerializedData.encode(val1, null, null)
//        println(data)
//        println(data.getMetaJavaTypeString())
//        val val2 = data.decode(null, null)
//
//        val2 shouldBe val1
//      }

      "Array of Map of Objects without serializer should be serializable / deserializable (with type)" {
        val val1 = arrayOf(mapOf(1 to Pojo1("42", 42, JType.TYPE_1)))
        val data = SerializedData.encode(val1, typeOf<Array<Map<Int, Pojo1>>>().javaType, null)
        val val2 = data.decode(typeOf<Array<Map<Int, Pojo1>>>().javaType, null)

        val2 shouldBe val1
      }
    },
)

