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

import io.infinitic.dashboard.Infinitic
import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.requests.Request
import io.infinitic.dashboard.slideovers.Slideover
import io.infinitic.pulsar.resources.TopicDescription
import kweb.Element
import kweb.ElementCreator
import kweb.div
import kweb.new
import kweb.state.KVar
import kweb.state.render
import kweb.table
import kweb.tbody
import kweb.td
import kweb.th
import kweb.thead
import kweb.tr
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

internal fun ElementCreator<Element>.displayJobStatsTable(
  name: String,
  state: KVar<out JobState<out TopicDescription>>,
  selectionSlide: Slideover<*>,
  selectionType: KVar<TopicDescription>,
  selectionStats: KVar<Request<PartitionedTopicStats>>
) {
  // Topics table
  div().classes("pt-5").new {
    div().classes("max-w-none mx-auto").new {
      div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
        div().classes("flex flex-col").new {
          div().classes("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8").new {
            div().classes("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8").new {
              div().classes("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg").new {
                render(state) { state ->
                  table().classes("min-w-full divide-y divide-gray-200").new {
                    thead().classes("bg-gray-50").new {
                      tr().new {
                        th()
                            .classes(
                                "px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider",
                            )
                            .set("scope", "col")
                            .text("Topic's type")
                        th()
                            .classes(
                                "px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider",
                            )
                            .set("scope", "col")
                            .text("# Consumers")
                        th()
                            .classes(
                                "px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider",
                            )
                            .set("scope", "col")
                            .text("Msg Backlog")
                        th()
                            .classes(
                                "px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider",
                            )
                            .set("scope", "col")
                            .text("Msg Rate Out")
                        th()
                            .classes(
                                "px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider",
                            )
                            .set("scope", "col")
                            .text("Topic's name")
                      }
                    }
                    tbody().new {
                      state.topicsStats.forEach {
                        val type = it.key
                        val request = it.value
                        val topic = Infinitic.resourceManager.getTopicName(name, type)
                        val row = tr()

                        when (request) {
                          is Loading ->
                            row.classes("bg-white").new {
                              td()
                                  .classes("px-6 py-4 text-sm font-medium text-gray-900")
                                  .text(type.subscriptionPrefix)
                              td().classes("px-6 py-4 text-sm text-gray-500").text("loading...")
                              td().classes("px-6 py-4 text-sm text-gray-500").text("loading...")
                              td().classes("px-6 py-4 text-sm text-gray-500").text("loading...")
                              td().classes("px-6 py-4 text-sm text-gray-500").text(topic)
                            }

                          is Failed ->
                            row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
                              td()
                                  .classes("px-6 py-4 text-sm font-medium text-gray-900")
                                  .text(type.subscriptionPrefix)
                              td()
                                  .classes("px-6 py-4 text-sm text-gray-500 text-center italic")
                                  .set("colspan", 3)
                                  .text(request.title)
                              td().classes("px-6 py-4 text-sm text-gray-500").text(topic)
                            }

                          is Completed ->
                            request.result.subscriptions.map { entry ->
                              row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
                                td()
                                    .classes("px-6 py-4 text-sm font-medium text-gray-900")
                                    .text(type.subscriptionPrefix)
                                td()
                                    .classes("px-6 py-4 text-sm text-gray-500")
                                    .text(entry.value.consumers.size.toString())
                                td()
                                    .classes("px-6 py-4 text-sm text-gray-500")
                                    .text(entry.value.msgBacklog.toString())
                                td()
                                    .classes("px-6 py-4 text-sm text-gray-500")
                                    .text("%.2f".format(entry.value.msgRateOut) + " msg/s")
                                td().classes("px-6 py-4 text-sm text-gray-500").text(topic)
                              }
                            }
                        }
                        if (request !is Loading<*>) {
                          row.on.click {
                            if (selectionType.value != type) {
                              selectionType.value = type
                              selectionStats.value = request
                            }

                            selectionSlide.open()
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
