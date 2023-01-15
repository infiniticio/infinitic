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
  fun prop1bis()
  fun prop2(): String
  fun prop2bis()
  fun prop3(): String
  fun prop3bis()
  fun prop4(): String
  fun prop4bis()
  fun prop5(): String
  fun prop5bis()
  fun prop5ter()
  fun prop6(): String
  fun prop6bis(deferred: Deferred<String>): String
  fun prop7(): String
  fun prop7bis(): String
  fun prop8(): String
  fun prop8bis()
}

@Suppress("unused")
class PropertiesWorkflowImpl : Workflow(), PropertiesWorkflow {

  @Ignore private val self by lazy { getWorkflowById(PropertiesWorkflow::class.java, workflowId) }

  lateinit var deferred: Deferred<String>

  private val utilService =
      newService(
          UtilService::class.java,
          tags = setOf("foo", "bar"),
          meta = mapOf("foo" to "bar".toByteArray()))

  private var p1 = ""

  override fun prop1(): String {
    p1 = "a"
    dispatch(self::prop1bis)
    p1 += "c"

    return p1 // should be "ac"
  }

  override fun prop1bis() {
    p1 += "b"
  }

  override fun prop2(): String {
    p1 = "a"
    dispatch(self::prop2bis)
    p1 += "c"
    utilService.await(100)
    p1 += "d"

    return p1 // should be "acbd"
  }

  override fun prop2bis() {
    p1 += "b"
  }

  override fun prop3(): String {
    p1 = "a"
    dispatch(self::prop3bis)
    p1 += "c"
    utilService.await(2000)
    p1 += "d"

    return p1 // should be "acbd"
  }

  override fun prop3bis() {
    utilService.await(50)
    p1 += "b"
  }

  override fun prop4(): String {
    p1 = "a"
    dispatch(self::prop4bis)
    p1 += "c"
    utilService.await(100)
    p1 += "d"

    return p1 // should be "acd"
  }

  override fun prop4bis() {
    utilService.await(200)
    p1 += "b"
  }

  override fun prop5(): String {
    p1 = "a"
    dispatch(self::prop5bis)
    dispatch(self::prop5ter)
    p1 += "d"
    utilService.await(100)

    return p1 // should be "adbc"
  }

  override fun prop5bis() {
    p1 += "b"
  }

  override fun prop5ter() {
    p1 += "c"
  }

  override fun prop6(): String {
    val d1 = dispatch(utilService::reverse, "12")
    val d2 = dispatch(self::prop6bis, d1)
    d1.await()
    p1 += "a"
    p1 = d2.await() + p1
    // unfortunately p1 = p1 + d2.await() would fail the test
    // because d2.await() updates p1 value too lately in the expression
    // not sure how to fix that or if it should be fixed

    return p1 // should be "abab"
  }

  override fun prop6bis(deferred: Deferred<String>): String {
    deferred.await()
    p1 += "b"
    return p1
  }

  override fun prop7(): String {
    deferred = dispatch(utilService::reverse, "12")
    val d2 = dispatch(self::prop7bis)
    deferred.await()
    p1 += "a"
    p1 = d2.await() + p1
    // unfortunately p1 = p1 + d2.await() would fail the test
    // because d2.await() updates p1 value too lately in the expression
    // not sure, how to fix that or if it should be fixed

    return p1 // should be "abab"
  }

  override fun prop7bis(): String {
    deferred.await()
    p1 += "b"
    return p1
  }

  override fun prop8(): String {
    p1 = utilService.reverse("a")
    dispatch(self::prop8bis)
    p1 += "c"
    utilService.await(200)
    p1 += "d"

    return p1 // should be "acbd"
  }

  override fun prop8bis() {
    p1 += utilService.reverse("b")
  }
}
