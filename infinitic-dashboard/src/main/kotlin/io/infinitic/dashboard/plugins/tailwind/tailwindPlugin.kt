/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.dashboard.plugins.tailwind

import kweb.plugins.KwebPlugin
import kweb.plugins.staticFiles.ResourceFolder
import kweb.plugins.staticFiles.StaticFilesPlugin
import org.jsoup.nodes.Document

private const val resourceFolder = "/css"
private const val resourceRoute = "/static/css"

class TailwindPlugin : KwebPlugin(
    dependsOn = setOf(
        StaticFilesPlugin(ResourceFolder(resourceFolder), resourceRoute)
    )
) {
    override fun decorate(doc: Document) {
        doc.head().appendElement("link")
            .attr("rel", "stylesheet")
            .attr("type", "text/css")
            .attr("href", "$resourceRoute/compiled.css")

        doc.head().appendElement("link")
            .attr("rel", "stylesheet")
            .attr("href", "https://rsms.me/inter/inter.css")
    }
}

val tailwindPlugin get() = TailwindPlugin()
