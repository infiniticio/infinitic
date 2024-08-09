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

package io.infinitic.common.serDe.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InferTypeTests : StringSpec(
    {
      "inferType should correctly infer the type of Array" {
        val array = arrayOf("a", "b", "c")
        array.inferJavaType().toString() shouldBe
            "[array type, component type: [simple type, class java.lang.String]]"
      }

      "inferType should correctly infer the type of Array of List" {
        val array = arrayOf(listOf("a", "b"), listOf("c", "d"), listOf("e", "f"))
        array.inferJavaType().toString() shouldBe
            "[array type, component type: [collection type; class java.util.Arrays\$ArrayList, contains [simple type, class java.lang.String]]]"
      }

      "inferType should correctly infer the type of Collection.singleton" {
        val list = listOf("a")
        list.inferJavaType().toString() shouldBe
            "[collection type; class java.util.Collections\$SingletonList, contains [simple type, class java.lang.String]]"
      }

      "inferType should correctly infer the type of Collection" {
        val list = listOf("a", "b", "c")
        list.inferJavaType().toString() shouldBe
            "[collection type; class java.util.Arrays\$ArrayList, contains [simple type, class java.lang.String]]"
      }

      "inferType should correctly infer the type of mutable Collection" {
        val list = mutableListOf("a", "b", "c")
        list.inferJavaType().toString() shouldBe
            "[collection type; class java.util.ArrayList, contains [simple type, class java.lang.String]]"
      }

      "inferType should correctly infer the type of Map" {
        val map = mapOf("a" to "b", "c" to "d")
        map.inferJavaType().toString() shouldBe
            "[map type; class java.util.LinkedHashMap, [simple type, class java.lang.String] -> [simple type, class java.lang.String]]"
      }

      "inferType should correctly infer the type of an Individual Object" {
        val str = "abcdef"
        str.inferJavaType().toString() shouldBe
            "[simple type, class java.lang.String]"
      }
    },
)
