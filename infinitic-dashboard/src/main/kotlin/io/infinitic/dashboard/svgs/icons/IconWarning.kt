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

fun ElementCreator<Element>.iconWarning(): Element {
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
                "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z")
      }

  return svg
}
