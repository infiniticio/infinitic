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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod

class ClassUtilTests : StringSpec(
    {

      val method: Method = Foo::bar.javaMethod!!
      val klass: Class<Foo> = Foo::class.java

      "Does not find non existent annotation" {
        method.findAnnotation(Unknown::class.java) shouldBe null

        klass.findAnnotation(Unknown::class.java) shouldBe null
      }

      "Find method annotation" {
        method.findAnnotation(Test1::class.java).shouldBeInstanceOf<Test1>()

        method.findAnnotation(Test1::class.java, false).shouldBeInstanceOf<Test1>()
      }

      "Find class annotation" {
        klass.findAnnotation(Test2::class.java).shouldBeInstanceOf<Test2>()

        klass.findAnnotation(Test2::class.java, false).shouldBeInstanceOf<Test2>()
      }

      "Find method annotation on interface" {
        method.findAnnotation(Test3::class.java).shouldBeInstanceOf<Test3>()

        method.findAnnotation(Test3::class.java, false) shouldBe null
      }

      "Find class annotation on interface" {
        klass.findAnnotation(Test4::class.java).shouldBeInstanceOf<Test4>()

        klass.findAnnotation(Test4::class.java, false) shouldBe null
      }

      "Find method annotation on parent" {
        method.findAnnotation(Test5::class.java).shouldBeInstanceOf<Test5>()

        method.findAnnotation(Test5::class.java, false).shouldBeInstanceOf<Test5>()
      }

      "Find class annotation on parent" {
        klass.findAnnotation(Test6::class.java).shouldBeInstanceOf<Test6>()

        klass.findAnnotation(Test6::class.java, false).shouldBeInstanceOf<Test6>()
      }

      "Find method annotation on parent interface" {
        method.findAnnotation(Test7::class.java).shouldBeInstanceOf<Test7>()

        method.findAnnotation(Test7::class.java, false) shouldBe null
      }

      "Find class annotation on parent interface" {
        klass.findAnnotation(Test8::class.java).shouldBeInstanceOf<Test8>()

        klass.findAnnotation(Test8::class.java, false) shouldBe null
      }
    },
)

@Test8
private interface FooParentInterface {
  @Test7
  fun bar(p: String): String
}

@Test6
private open class FooParent : FooParentInterface {
  @Test5
  override fun bar(p: String) = p
}

@Test4
private interface FooInterface {
  @Test3
  fun bar(p: String): String
}

@Test2
private class Foo : FooParent(), FooInterface {
  @Test1
  override fun bar(p: String) = p
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Unknown()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test1()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test2()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test3()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test4()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test5()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test6()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test7()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
private annotation class Test8()
