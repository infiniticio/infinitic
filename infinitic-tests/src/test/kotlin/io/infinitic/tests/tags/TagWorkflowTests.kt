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
package io.infinitic.tests.tags

import io.infinitic.tests.Test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

internal class TagWorkflowTests : StringSpec(
    {
      val client = Test.client

      "check tags from workflow context" {
        val tagWorkflow = client.newWorkflow(TagWorkflow::class.java, tags = setOf("foo", "bar"))

        tagWorkflow.context() shouldBe setOf("foo", "bar")
      }

      "target workflow by tag" {
        val tagWorkflow = client.newWorkflow(TagWorkflow::class.java, tags = setOf("foo", "bar"))

        client.dispatch(tagWorkflow::await)

        val w = client.getWorkflowByTag(TagWorkflow::class.java, "foo")
        client.getIds(w).size shouldBe 1

        client.cancel(w)
        delay(500)
        client.getIds(w).size shouldBe 0
      }

      "when dispatching twice a workflow with the same customId, the second call should be discarded" {
        val tagWorkflow = client.newWorkflow(
            TagWorkflow::class.java,
            tags = setOf("foo", "customId:bar"),
        )

        client.dispatch(tagWorkflow::await)
        client.dispatch(tagWorkflow::await)

        val w = client.getWorkflowByTag(TagWorkflow::class.java, "foo")
        client.getIds(w).size shouldBe 1

        client.cancel(w)
        delay(500)
        client.getIds(w).size shouldBe 0
      }
    },
)
