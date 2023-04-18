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
package io.infinitic.dashboard.svgs.icons

import io.infinitic.dashboard.svgs.path
import io.infinitic.dashboard.svgs.svg
import kweb.Element
import kweb.ElementCreator
import kweb.new

fun ElementCreator<Element>.iconRefresh(): Element {
  val svg = svg()
  svg.setClasses("h-6 w-6")
      .set("fill", "none")
      .set("viewBox", "0 0 24 24")
      .set("stroke", "currentColor")
      .new {
        path()
            .set("stroke-linecap", "round")
            .set("stroke-linejoin", "round")
            .set("stroke-width", "2")
            .set(
                "d",
                "M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15")
      }

  return svg
}
