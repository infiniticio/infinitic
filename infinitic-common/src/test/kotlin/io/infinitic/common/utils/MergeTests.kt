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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MergeTests : StringSpec(
    {
      "Can merge 2 data classes" {
        val oA = Data(p1 = "p1A", p4 = "p4A")
        val oB = Data(p1 = "p1B", p2 = "p2B", p5 = setOf("p5B"))

        (oA merge oB) shouldBe Data(p1 = "p1A", p2 = "p2B", p4 = "p4A", p5 = setOf("p5B"))
      }
    },
)

internal data class Data(
  val p1: String? = null,
  val p2: String? = null,
  val p3: String? = null,
  val p4: String? = null,
  val p5: Set<String>? = null
)
