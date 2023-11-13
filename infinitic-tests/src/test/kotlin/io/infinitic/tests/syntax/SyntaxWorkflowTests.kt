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

import io.infinitic.tests.WorkflowTests
import io.infinitic.tests.utils.AnnotatedWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class SyntaxWorkflowTests :
  StringSpec(
      {
        // each test should not be longer than 5s
        timeout = 5000

        val tests = WorkflowTests()
        val worker = tests.worker
        val client = tests.client

        val syntaxWorkflow = client.newWorkflow(SyntaxWorkflow::class.java)
        val annotatedWorkflow = client.newWorkflow(AnnotatedWorkflow::class.java)

        beforeSpec { worker.startAsync() }

        afterSpec {
          worker.close()
          client.close()
        }

        beforeTest { worker.registry.flush() }

        "empty Workflow" { syntaxWorkflow.empty() shouldBe "void" }

        "run task from parent interface" { syntaxWorkflow.parent() shouldBe "ok" }

        "run childWorkflow from parent interface" { syntaxWorkflow.wparent() shouldBe "ok" }

        //    "Tag should be added then deleted after completion" {
        //        val deferred = client.dispatch(workflowATagged::channel1)
        //
        //        val w = client.getWorkflowByTag(WorkflowA::class.java, "foo")
        //        client.getIds(w).contains(deferred.id) shouldBe true
        //
        //        // complete workflow
        //        w.channelA.send("")
        //
        //        // delay is necessary to be sure that tag engine has processed
        //        delay(500)
        //
        //        client.getIds(w).contains(deferred.id) shouldBe false
        //    }

        "Annotated Workflow" {
          val result = annotatedWorkflow.concatABC("")

          result shouldBe "abc"
        }
      },
  )
