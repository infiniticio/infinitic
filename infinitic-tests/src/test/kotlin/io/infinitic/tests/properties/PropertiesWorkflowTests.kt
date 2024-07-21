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

import io.infinitic.Test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class PropertiesWorkflowTests :
  StringSpec(
      {
        val client = Test.client

        val propertiesWorkflow = client.newWorkflow(PropertiesWorkflow::class.java)

        "Check prop1" { propertiesWorkflow.prop1() shouldBe "ac" }

        "Check prop2" { propertiesWorkflow.prop2() shouldBe "acbd" }

        "Check prop3" { propertiesWorkflow.prop3() shouldBe "acbd" }

        "Check prop4" { propertiesWorkflow.prop4() shouldBe "acd" }

        "Check prop5" { propertiesWorkflow.prop5() shouldBe "adbc" }

        "Check prop6" { propertiesWorkflow.prop6() shouldBe "abab" }

        "Check prop7" { propertiesWorkflow.prop7() shouldBe "abab" }

        "Check prop8" { propertiesWorkflow.prop8() shouldBe "acbd" }
      },
  )
