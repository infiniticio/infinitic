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
package io.infinitic.tests.properties

import io.infinitic.annotations.Ignore
import io.infinitic.tests.utils.UtilService
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow

interface PropertiesWorkflow {
  fun prop1(): String

  fun propP1Plus(value: String, delay: Long? = null)

  fun prop2(): String

  fun prop3(): String

  fun prop4(): String

  fun prop5(): String

  fun prop6(): String

  fun prop6bis(): String

  fun prop6other(deferred: Deferred<String>): String

  fun prop7(): String

  fun prop7bis(): String

  fun prop7other(): String

  fun prop8(): String

  fun prop8bis()
}

@Suppress("unused")
class PropertiesWorkflowImpl : Workflow(), PropertiesWorkflow {

  @Ignore
  private val self by lazy { getWorkflowById(PropertiesWorkflow::class.java, workflowId) }

  lateinit var deferred: Deferred<String>

  private val utilService = newService(UtilService::class.java)

  private var p1 = ""

  override fun propP1Plus(value: String, delay: Long?) {
    delay?.let { utilService.await(it) }
    p1 += value
  }

  override fun prop1(): String {
    p1 = "a"
    self.propP1Plus("b")
    p1 += "c"

    return p1 // should be "abc"
  }

  override fun prop2(): String {
    p1 = "a"
    val d = dispatch(self::propP1Plus, "b")
    p1 += "c"
    d.await()
    p1 += "d"

    return p1 // should be "acbd"
  }

  override fun prop3(): String {
    p1 = "a"
    val d = dispatch(self::propP1Plus, "b", 10)
    p1 += "c"
    d.await()
    p1 += "d"

    return p1 // should be "acbd"
  }

  override fun prop4(): String {
    p1 = "a"
    dispatch(self::propP1Plus, "b", 200)
    p1 += "c"
    utilService.await(10)
    p1 += "d"

    return p1 // should be "acd"
  }

  override fun prop5(): String {
    p1 = "a"
    val d1 = dispatch(self::propP1Plus, "b", 10)
    val d2 = dispatch(self::propP1Plus, "c")
    p1 += "d"
    d1.and(d2).await()

    return p1 // should be "adcb"
  }

  override fun prop6(): String {
    val d1 = dispatch(utilService::reverse, "12")
    val d2 = dispatch(self::prop6other, d1)
    d1.await()
    p1 += "a"
    p1 = d2.await() + p1
    // unfortunately p1 = p1 + d2.await() would fail the test
    // because d2.await() updates p1 value too lately in the expression
    // not sure how to fix that or if it should be fixed

    return p1 // should be "abab"
  }

  override fun prop6bis(): String {
    val d1 = dispatch(utilService::reverse, "12")
    d1.await()
    val d2 = dispatch(self::prop6other, d1)
    p1 += "a"
    p1 = d2.await() + p1
    // unfortunately p1 = p1 + d2.await() would fail the test
    // because d2.await() updates p1 value too lately in the expression
    // not sure how to fix that or if it should be fixed

    return p1 // should be "abab"
  }

  override fun prop6other(deferred: Deferred<String>): String {
    deferred.await()
    p1 += "b"
    return p1
  }

  override fun prop7(): String {
    deferred = dispatch(utilService::reverse, "12")
    val d2 = dispatch(self::prop7other)
    deferred.await()
    p1 += "a"
    p1 = d2.await() + p1
    // unfortunately p1 = p1 + d2.await() would fail the test
    // because d2.await() updates p1 value too lately in the expression
    // not sure, how to fix that or if it should be fixed

    return p1 // should be "abab"
  }

  override fun prop7bis(): String {
    deferred = dispatch(utilService::reverse, "12")
    deferred.await()

    val d2 = dispatch(self::prop7other)
    p1 += "a"
    p1 = d2.await() + p1
    // unfortunately p1 = p1 + d2.await() would fail the test
    // because d2.await() updates p1 value too lately in the expression
    // not sure, how to fix that or if it should be fixed

    return p1 // should be "abab"
  }

  override fun prop7other(): String {
    deferred.await()
    p1 += "b"
    return p1
  }

  override fun prop8(): String {
    p1 = utilService.reverse("a")
    val d = dispatch(self::prop8bis)
    p1 += "c"
    d.await()
    p1 += "d"

    return p1 // should be "acbd"
  }

  override fun prop8bis() {
    p1 += utilService.reverse("b")
  }
}
