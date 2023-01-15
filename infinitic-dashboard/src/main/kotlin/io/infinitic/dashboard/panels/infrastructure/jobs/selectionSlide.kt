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
package io.infinitic.dashboard.panels.infrastructure.jobs

import io.infinitic.common.serDe.json.Json
import io.infinitic.dashboard.panels.infrastructure.lastUpdated
import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.requests.Request
import io.infinitic.dashboard.slideovers.Slideover
import io.infinitic.transport.pulsar.topics.TopicType
import kweb.a
import kweb.new
import kweb.p
import kweb.span
import kweb.state.KVar
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

internal fun selectionSlide(
    selectionType: KVar<TopicType>,
    selectionStats: KVar<Request<PartitionedTopicStats>>
) =
    Slideover(
        selectionType.map {
          "${it.subscriptionPrefix} stats".replaceFirstChar { c -> c.uppercase() }
        },
        selectionStats) {
          p().classes("text-sm font-medium text-gray-900").new {
            span().text(lastUpdated(it.value.lastUpdated) + " (cf.")
            a().classes("underline")
                .setAttribute(
                    "href",
                    "https://pulsar.apache.org/docs/en/next/administration-stats/#partitioned-topics")
                .setAttribute("target", "_blank")
                .text("documentation")
            span().text(")")
          }
          p().classes("mt-7 text-sm text-gray-500").new {
            element("pre")
                .text(
                    when (val request = it.value) {
                      is Loading -> "Loading..."
                      is Failed -> request.error.stackTraceToString()
                      is Completed -> Json.stringify(request.result, true)
                    })
          }
        }
