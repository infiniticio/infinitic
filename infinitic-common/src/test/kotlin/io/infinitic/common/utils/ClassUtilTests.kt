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

import io.infinitic.annotations.Name
import io.infinitic.annotations.Timeout
import io.infinitic.common.data.MillisDuration
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
import io.infinitic.tasks.WithTimeout
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
      }

      "Find class annotation" {
        klass.findAnnotation(Test2::class.java).shouldBeInstanceOf<Test2>()
      }

      "Find method annotation on interface" {
        method.findAnnotation(Test3::class.java).shouldBeInstanceOf<Test3>()
      }

      "Find class annotation on interface" {
        klass.findAnnotation(Test4::class.java).shouldBeInstanceOf<Test4>()
      }

      "Find method annotation on parent" {
        method.findAnnotation(Test5::class.java).shouldBeInstanceOf<Test5>()
      }

      "Find class annotation on parent" {
        klass.findAnnotation(Test6::class.java).shouldBeInstanceOf<Test6>()
      }

      "Find method annotation on parent interface" {
        method.findAnnotation(Test7::class.java).shouldBeInstanceOf<Test7>()
      }

      "Find class annotation on parent interface" {
        klass.findAnnotation(Test8::class.java).shouldBeInstanceOf<Test8>()
      }

      "Should be able to find name annotation on interface" {
        method.annotatedName shouldBe "barAnnotated"
      }

      "Interface name with annotation should be annotation" {
        FooParentInterface::class.java.annotatedName shouldBe "FooParentInterfaceAnnotated"
      }

      "Class name without annotation should be interface name with annotation" {
        FooParent::class.java.annotatedName shouldBe "FooParentInterfaceAnnotated"
      }

      "Class name with annotation should be annotation name" {
        Foo::class.java.annotatedName shouldBe "FooAnnotatedAnnotated"
      }

      "Class name with annotation should be class name" {
        BarImpl::class.java.annotatedName shouldBe BarImpl::class.java.name
      }

      "can read timeout from interface with default" {
        TrueBar::getTimeoutSeconds.javaMethod!!.getMillisDuration(TrueBar::class.java)
            .getOrThrow() shouldBe MillisDuration(1000L)
      }

      "can not read timeout from interface without default" {
        Bar::getTimeoutSeconds.javaMethod!!.getMillisDuration(Bar::class.java)
            .getOrThrow() shouldBe null
      }

      "can read timeout from object implementing WithTimeout" {
        BarImpl::foo.javaMethod!!.getMillisDuration(BarImpl::class.java)
            .getOrThrow() shouldBe MillisDuration(1000L)
      }

      "can read timeout from annotation" {
        TrueBarImpl::foo.javaMethod!!.getMillisDuration(TrueBarImpl::class.java)
            .getOrThrow() shouldBe MillisDuration(10)
      }

      "Find parameter annotation" {
        FooParentInterface::bar.javaMethod
            ?.findParameterAnnotation(Parameter::class.java, 0).shouldBeInstanceOf<Parameter>()
      }

      "Find parameter annotation on parent interface" {
        val annotation = FooParent::bar.javaMethod
            ?.findParameterAnnotation(Parameter::class.java, 0)
        annotation.shouldBeInstanceOf<Parameter>()
        annotation.name shouldBe ""
      }

      "Find parameter annotation on parent" {
        val annotation = Foo2::bar.javaMethod
            ?.findParameterAnnotation(Parameter::class.java, 0)
        annotation.shouldBeInstanceOf<Parameter>()
        annotation.name shouldBe "2"
      }

      "Full method name should not change (as it could trigger false positive in change detection)" {
        klass.getFullMethodName(method) shouldBe "FooAnnotatedAnnotated::barAnnotated"

        val k1 = Foo2::class.java
        val m1 = k1.getMethod("bar", String::class.java)
        k1.getFullMethodName(m1) shouldBe "FooInterfaceAnnotated::barMethodInterface"

        val k2 = Foo3::class.java
        val m2 = k2.getMethod("bar", String::class.java)
        k2.getFullMethodName(m2) shouldBe "Foo3::bar"
      }

      "getMethodPerNameAndParameters should throw" {
        shouldThrow<NoMethodFoundWithParameterTypesException> {
          klass.getMethodPerNameAndParameters(
              "unknown",
              listOf(String::class.java.name),
              1,
          )
        }

        shouldThrow<NoMethodFoundWithParameterTypesException> {
          klass.getMethodPerNameAndParameters(
              "bar",
              listOf(Object::class.java.name),
              1,
          ) shouldNotBe null
        }

        shouldThrow<NoMethodFoundWithParameterTypesException> {
          klass.getMethodPerNameAndParameters(
              "barAnnotated",
              listOf(Object::class.java.name),
              1,
          ) shouldNotBe null
        }
      }

      "getMethodPerNameAndParameters should return method" {
        klass.getMethodPerNameAndParameters(
            "bar",
            listOf(String::class.java.name),
            1,
        ) shouldNotBe null

        klass.getMethodPerNameAndParameters(
            "barAnnotated",
            listOf(String::class.java.name),
            1,
        ) shouldNotBe null

      }
    },
)

@Test8
@Name("FooParentInterfaceAnnotated")
private interface FooParentInterface {
  @Test7
  @Name("barMethodInterface")
  fun bar(@Parameter p: String): String
}

@Test6
private open class FooParent : FooParentInterface {
  @Test5
  override fun bar(p: String) = p
}

@Test4
@Name("FooInterfaceAnnotated")
private interface FooInterface {
  @Test3
  fun bar(p: String): String
}

@Test2
@Name("FooAnnotatedAnnotated")
private class Foo : FooParent(), FooInterface {
  @Test1
  @Name("barAnnotated")
  override fun bar(p: String) = p
}

private class Foo2 : FooParent(), FooInterface {
  override fun bar(@Parameter("2") p: String) = p
}

private class Foo3 {
  fun bar(p: String) = p
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

@Target(AnnotationTarget.VALUE_PARAMETER)
private annotation class Parameter(val name: String = "")

private interface Bar : WithTimeout {
  fun foo()
}

private class BarImpl : Bar {
  override fun foo() {}

  override fun getTimeoutSeconds() = 1.0
}

private interface TrueBar : WithTimeout {
  fun foo()
  override fun getTimeoutSeconds() = 1.0
}

private class TrueBarImpl : TrueBar {
  @Timeout(with = After10MilliSeconds::class)
  override fun foo() {
  }
}

class After10MilliSeconds : WithTimeout {
  override fun getTimeoutSeconds() = 0.01
}
