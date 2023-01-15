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

fun ElementCreator<Element>.iconHome(): Element {
  val svg = svg()
  svg.setClasses("h-6 w-6")
      .setAttribute("fill", "none")
      .setAttribute("viewBox", "0 0 24 24")
      .setAttribute("stroke", "currentColor")
      .new {
        path()
            .setAttribute("stroke-linecap", "round")
            .setAttribute("stroke-linejoin", "round")
            .setAttribute("stroke-width", "2")
            .setAttribute(
                "d",
                "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6")
      }

  return svg
}
