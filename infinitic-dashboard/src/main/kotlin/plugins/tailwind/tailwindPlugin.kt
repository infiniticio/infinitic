package plugins.tailwind

import org.jsoup.nodes.Document
import kweb.plugins.KwebPlugin
import kweb.plugins.staticFiles.ResourceFolder
import kweb.plugins.staticFiles.StaticFilesPlugin

private const val resourceFolder = "/css"
private const val resourceRoute = "/static/css"

class TailwindPlugin : KwebPlugin(dependsOn = setOf(
    StaticFilesPlugin(ResourceFolder(resourceFolder), resourceRoute)
)) {
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
