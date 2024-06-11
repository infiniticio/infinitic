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
package io.infinitic.common.serDe

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.infinitic.common.serDe.json.Json
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SerDeJacksonTests :
  StringSpec(
      {
        "Null (Obj) should be serializable / deserializable" {
          val obj1: JObj1? = null
          val obj2 = SerializedData.from(obj1).deserialize()

          obj2 shouldBe obj1
        }

        "Simple Object should be serializable / deserializable" {
          val val1 = JObj1("42", 42, JType.TYPE_1)
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }

        "Object should be deserializable with more properties" {
          val val1 = JObj1("42", 42, JType.TYPE_1)
          val data =
              SerializedData.from(val1)
                  .copy(
                      meta =
                      mapOf(
                          SerializedData.META_JAVA_CLASS to
                              JObj2::class.java.name.toByteArray(charset = Charsets.UTF_8),
                      ),
                  )
          val val2 = data.deserialize() as JObj2

          val2.foo shouldBe val1.foo
          val2.bar shouldBe val1.bar
        }

        "Object should be deserializable with less properties using default value" {
          val val2 = JObj2("42", 42)
          val data =
              SerializedData.from(val2)
                  .copy(
                      meta =
                      mapOf(
                          SerializedData.META_JAVA_CLASS to
                              JObj1::class.java.name.toByteArray(charset = Charsets.UTF_8),
                      ),
                  )
          val val1 = data.deserialize() as JObj1

          val1.foo shouldBe val2.foo
          val1.bar shouldBe val2.bar
          val1.type shouldBe JType.TYPE_1
        }

        "Object containing set of sealed should be serializable / deserializable (even with a 'type' property)" {
          val val1 = JObjs(setOf(JObj1("42", 42, JType.TYPE_1)))
          val val2 = SerializedData.from(val1).deserialize()

          val2 shouldBe val1
        }


        "Can alter Json.mapper" {
          // this serialization is broken due to "Foo" attribute instead of "foo"
          val jsonTxt = "{\"Foo\":\"foo\",\"bar\":42,\"type\":\"TYPE_1\"}"
          shouldThrowAny { Json.parse(jsonTxt, klass = JObj1::class.java) }
          // we replace the mapper to clear cache and configure ACCEPT_CASE_INSENSITIVE_PROPERTIES
          Json.mapper = jsonMapper {
            addModule(KotlinModule.Builder().build())
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
          }
          shouldNotThrowAny { Json.parse(jsonTxt, klass = JObj1::class.java) }
        }
      },
  )


@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
private sealed class JObj

private enum class JType {
  TYPE_1,
}

private data class JObj1(val foo: String, val bar: Int, val type: JType = JType.TYPE_1) : JObj()

private data class JObj2(val foo: String, val bar: Int) : JObj()

private data class JObjs(val objs: Set<JObj>)
