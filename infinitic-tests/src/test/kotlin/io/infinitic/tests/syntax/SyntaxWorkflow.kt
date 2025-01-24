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
package io.infinitic.tests.syntax

import io.infinitic.utils.ParentInterface
import io.infinitic.utils.UtilService
import io.infinitic.workflows.Workflow
import kotlinx.serialization.Serializable

internal interface SyntaxWorkflow : ParentInterface {
  fun empty(): String

  fun await(duration: Long): Long

  fun wparent(): String

  fun polymorphism(): Parent
}

@Suppress("unused")
internal class SyntaxWorkflowImpl : Workflow(), SyntaxWorkflow {

  private val utilService =
      newService(
          UtilService::class.java,
          tags = setOf("foo", "bar"),
          meta = mutableMapOf("foo" to "bar".toByteArray()),
      )
  private val syntaxWorkflow = newWorkflow(SyntaxWorkflow::class.java)

  private var p1 = ""

  override fun empty() = "void"

  override fun await(duration: Long) = utilService.await(duration)

  override fun parent() = utilService.parent()

  override fun wparent(): String = syntaxWorkflow.parent()

  override fun polymorphism() = utilService.polymorphism()
}

@Serializable
internal sealed class Parent {
  abstract val type: String
}

@Serializable
internal data class Child1(val child: String, override val type: String = "Child1") : Parent()

@Serializable
internal data class Child2(val child: String, override val type: String = "Child2") : Parent()
