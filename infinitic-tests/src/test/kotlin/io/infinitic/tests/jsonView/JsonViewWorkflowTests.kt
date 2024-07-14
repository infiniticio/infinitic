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
package io.infinitic.tests.jsonView

import io.infinitic.tests.Test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class JsonViewWorkflowTests : StringSpec(
    {
      val userId = "123"
      val password = "@&é'('"
      val company = "Acme"
      val request = Request("123").apply { this.company = company; this.password = password }

      val client = Test.client

      val jsonViewWorkflow = client.newWorkflow(JsonViewWorkflow::class.java)

      "without JsonView" {
        val user = jsonViewWorkflow.getUser_00(request)
        user shouldBe User(userId).apply {
          this.company = request.company; this.password = request.password
        }
      }

      "with JsonView on Service parameter" {
        val user = jsonViewWorkflow.getUser_01(request)
        user shouldBe User(userId).apply { this.company = null; this.password = request.password }
      }

      "with JsonView on Service return value" {
        val user = jsonViewWorkflow.getUser_02(request)
        user shouldBe User(userId).apply { this.company = request.company; this.password = null }
      }

      "with JsonView on both Service parameter and return value" {
        val user = jsonViewWorkflow.getUser_03(request)
        user shouldBe User(userId).apply { this.company = null; this.password = null }
      }

      "with JsonView on Workflow parameter" {
        val user = jsonViewWorkflow.getUser_10(request)
        user shouldBe User(userId).apply { this.company = null; this.password = request.password }
      }

      "with JsonView on Workflow return value" {
        val user = jsonViewWorkflow.getUser_20(request)
        user shouldBe User(userId).apply { this.company = request.company; this.password = null }
      }

      "with JsonView on both Workflow parameter and return value" {
        val user = jsonViewWorkflow.getUser_30(request)
        user shouldBe User(userId).apply { this.company = null; this.password = null }
      }
    },
)
