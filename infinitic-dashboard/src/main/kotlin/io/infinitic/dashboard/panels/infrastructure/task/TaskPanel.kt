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
package io.infinitic.dashboard.panels.infrastructure.task

import io.infinitic.dashboard.InfiniticDashboard
import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.menus.InfraMenu
import io.infinitic.dashboard.panels.infrastructure.AllJobsPanel
import io.infinitic.dashboard.panels.infrastructure.jobs.displayJobSectionHeader
import io.infinitic.dashboard.panels.infrastructure.jobs.displayJobStatsTable
import io.infinitic.dashboard.panels.infrastructure.jobs.selectionSlide
import io.infinitic.dashboard.panels.infrastructure.jobs.update
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.requests.Request
import io.infinitic.dashboard.svgs.icons.iconChevron
import io.infinitic.pulsar.resources.ServiceType
import io.infinitic.pulsar.resources.TopicType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.a
import kweb.div
import kweb.h2
import kweb.li
import kweb.nav
import kweb.new
import kweb.ol
import kweb.p
import kweb.span
import kweb.state.KVar
import kweb.state.property
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.util.concurrent.ConcurrentHashMap

class TaskPanel private constructor(private val taskName: String) : Panel() {

  companion object {
    const val template = "/infra/t/{name}"

    private val instances: ConcurrentHashMap<String, TaskPanel> = ConcurrentHashMap()

    fun from(taskName: String) = instances.computeIfAbsent(taskName) { TaskPanel(taskName) }
  }

  override val menu = InfraMenu

  override val url = "/infra/t/$taskName"

  private val state = KVar(TaskState(taskName))

  private val lastUpdated = state.property(TaskState::lastUpdatedAt)
  private val isLoading = state.property(TaskState::isLoading)

  private val selectionTopicType: KVar<TopicType> = KVar(ServiceType.EXECUTOR)
  private val selectionTopicStats: KVar<Request<PartitionedTopicStats>> = KVar(Loading())

  private val selectionSlide = selectionSlide(selectionTopicType, selectionTopicStats)

  lateinit var job: Job

  init {
    // making sure slideover content is updated
    state.addListener { _, new ->
      selectionTopicStats.value = new.topicsStats[selectionTopicType.value]!!
    }
  }

  override fun onEnter() {
    if (!this::job.isInitialized || job.isCancelled) {
      job =
          InfiniticDashboard.scope.launch {
            // update of task names every 30 seconds
            update(state)
          }
    }
  }

  override fun onLeave() {
    if (this::job.isInitialized) {
      job.cancel()
    }
    selectionSlide.close()
  }

  override fun render(creator: ElementCreator<Element>): Unit =
      with(creator) {
        // PAGE HEADER
        div().classes("bg-white shadow py-8").new {
          div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
            div().classes("lg:flex lg:items-center lg:justify-between").new {
              div().classes("flex-1 min-w-0").new {
                // breadcrumbs
                nav().classes("flex").set("aria-label", "Breadcrumb").new {
                  ol().classes("flex items-center space-x-4").set("role", "list").new {
                    li {
                      div().classes("flex items-center").new {
                        with(a()) {
                          classes("text-sm font-medium text-gray-500 hover:text-gray-700")
                          set("aria-current", InfraMenu.title)
                          text(InfraMenu.title)
                          href = AllJobsPanel.url
                        }
                        span().classes("sr-only").text("Infrastructure")
                      }
                    }
                    li {
                      div().classes("flex items-center").new {
                        iconChevron().classes("flex-shrink-0 h-5 w-5 text-gray-400")
                      }
                    }
                  }
                }
                // title
                h2()
                    .classes(
                        "mt-2 text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate",
                    )
                    .text(taskName)
              }
            }
          }
        }
        // TASK TOPICS
        div().classes("pt-8 pb-8").new {
          div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
            displayJobSectionHeader("Task's topics", isLoading, lastUpdated)
            p().classes("mt-7 text-sm text-gray-500").new {
              span()
                  .text("Here are the topics used for this task.")
                  .addText(" Click on a row to get more details on its real-time stats.")
            }
            displayJobStatsTable(
                taskName, state, selectionSlide, selectionTopicType, selectionTopicStats,
            )
          }
        }

        selectionSlide.render(this)
      }
}
