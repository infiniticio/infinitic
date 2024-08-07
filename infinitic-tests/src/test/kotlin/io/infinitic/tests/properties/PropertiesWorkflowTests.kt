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

import io.infinitic.tests.Test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class PropertiesWorkflowTests :
  StringSpec(
      {
        val client = Test.client

        val propertiesWorkflow = client.newWorkflow(PropertiesWorkflow::class.java)

        "Share data between branches" {
          propertiesWorkflow.prop1() shouldBe "abc"
        }

        "Share data between branches processed in parallel" {
          propertiesWorkflow.prop2() shouldBe "acbd"
        }

        "Share data between branches processed in parallel (multiple steps)" {
          propertiesWorkflow.prop3() shouldBe "acbd"
        }

        "Share data between multiple branches processed in parallel (time)" {
          propertiesWorkflow.prop4() shouldBe "acd"
        }

        "Check prop5" {
          propertiesWorkflow.prop5() shouldBe "adcb"
        }

        "With Deferred as branch argument" {
          propertiesWorkflow.prop6() shouldBe "abab"
        }

        "With Deferred as property" {
          propertiesWorkflow.prop7() shouldBe "abab"
        }

        "Check prop8" {
          propertiesWorkflow.prop8() shouldBe "acbd"
        }
      },
  )
