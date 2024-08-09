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
package io.infinitic.tests.children

import io.infinitic.Test
import io.infinitic.utils.UtilWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

internal class ChildrenWorkflowTests :
  StringSpec(
      {
        val client = Test.client

        val childrenWorkflow =
            client.newWorkflow(ChildrenWorkflow::class.java, tags = setOf("foo", "bar"))
        val utilWorkflow = client.newWorkflow(UtilWorkflow::class.java)

        "run task from parent interface" {
          childrenWorkflow.parent() shouldBe "ok"
        }

        "run childWorkflow from parent interface" {
          childrenWorkflow.wparent() shouldBe "ok"
        }

        "Sequential Child Workflow" {
          childrenWorkflow.child1() shouldBe "ab"
        }

        "Asynchronous Child Workflow" {
          childrenWorkflow.child2() shouldBe "baba"
        }

        "Nested Child Workflow" {
          utilWorkflow.factorial(14) shouldBe 87178291200
        }

        "Child workflow is canceled when parent workflow is canceled - tag are also added and deleted" {
          client.dispatch(childrenWorkflow::cancel)

          delay(1000)
          val w = client.getWorkflowByTag(ChildrenWorkflow::class.java, "foo")
          val size = client.getIds(w).size

          client.cancel(w)

          delay(1000)
          client.getIds(w).size shouldBe size - 2
        }
      },
  )
