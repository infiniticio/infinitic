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

fun ElementCreator<Element>.iconPulsar(): Element {
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
                "M24 8.925h-5.866c-1.586-3.041-3.262-5.402-5.544-5.402-2.97 0-4.367 2.593-5.717 5.115l-.118.22H0v1.5h3.934c1.39 0 1.673.468 1.673.468-1.09 1.691-2.4 3.363-4.584 3.363H0v1.574h1.03c4.234 0 6.083-3.434 7.567-6.193 1.361-2.541 2.31-4.08 3.993-4.08 1.747 0 3.584 3.801 5.201 7.157.237.488.477.988.72 1.483-6.2.197-9.155 1.649-11.559 2.833-1.759.866-3.147 1.94-5.433 1.94H0v1.574h1.507c2.754 0 4.47-.85 6.295-1.751 2.53-1.243 5.398-2.652 12.157-2.652h3.907V14.5H21.66a1.18 1.18 0 01-.972-.393 70.83 70.83 0 01-1.133-2.321l-.511-1.047s.366-.393 1.38-.393H24Z")
      }

  return svg
}
