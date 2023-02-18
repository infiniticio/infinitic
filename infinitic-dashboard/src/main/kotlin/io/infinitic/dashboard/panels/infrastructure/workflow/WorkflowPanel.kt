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
package io.infinitic.dashboard.panels.infrastructure.workflow

import io.infinitic.dashboard.InfiniticDashboard
import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.menus.InfraMenu
import io.infinitic.dashboard.panels.infrastructure.AllJobsPanel
import io.infinitic.dashboard.panels.infrastructure.jobs.JobState
import io.infinitic.dashboard.panels.infrastructure.jobs.displayJobSectionHeader
import io.infinitic.dashboard.panels.infrastructure.jobs.displayJobStatsTable
import io.infinitic.dashboard.panels.infrastructure.jobs.selectionSlide
import io.infinitic.dashboard.panels.infrastructure.jobs.update
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.requests.Request
import io.infinitic.dashboard.svgs.icons.iconChevron
import io.infinitic.transport.pulsar.topics.TopicType
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
import io.infinitic.transport.pulsar.topics.WorkflowTopics
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

class WorkflowPanel private constructor(private val workflowName: String) : Panel() {
  companion object {
    private val instances: ConcurrentHashMap<String, WorkflowPanel> = ConcurrentHashMap()

    fun from(workflowName: String) =
        instances.computeIfAbsent(workflowName) { WorkflowPanel(workflowName) }
  }

  override val menu = InfraMenu

  override val url = "/infra/w/$workflowName"

  private val workflowState = KVar(WorkflowState(workflowName))
  private val workflowIsLoading = workflowState.property(WorkflowState::isLoading)
  private val workflowLastUpdated = workflowState.property(WorkflowState::lastUpdatedAt)

  private val workflowTaskState = KVar(WorkflowTaskState(workflowName))
  private val workflowTaskIsLoading = workflowTaskState.property(WorkflowTaskState::isLoading)
  private val workflowTaskLastUpdated = workflowTaskState.property(WorkflowTaskState::lastUpdatedAt)

  private val selectionTopicType: KVar<TopicType> = KVar(WorkflowTopics.ENGINE)
  private val selectionTopicStats: KVar<Request<PartitionedTopicStats>> = KVar(Loading())

  private val selectionSlide = selectionSlide(selectionTopicType, selectionTopicStats)

  lateinit var job: Job

  init {
    // this listener ensures that the slideover appear/disappear with right content
    workflowState.addListener { _, new ->
      if (selectionTopicType.value is WorkflowTopics) {
        selectionTopicStats.value = new.topicsStats[selectionTopicType.value]!!
      }
    }

    // this listener ensures that the slideover appear/disappear with right content
    workflowTaskState.addListener { _, new ->
      if (selectionTopicType.value is WorkflowTaskTopics) {
        selectionTopicStats.value = new.topicsStats[selectionTopicType.value]!!
      }
    }
  }

  override fun onEnter() {
    if (!this::job.isInitialized || job.isCancelled) {
      job =
          InfiniticDashboard.scope.launch {
            // update of workflow task's topics every 30 seconds
            update(workflowState)
            // shift the updates
            delay(2000)
            // update of workflow's topics every 30 seconds
            update(workflowTaskState)
          }
    }
  }

  override fun onLeave() {
    if (this::job.isInitialized) {
      job.cancel()
    }
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
                        span().classes("sr-only").text(InfraMenu.title)
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
                        "mt-2 text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate")
                    .text(workflowName)
              }
            }
          }
        }

        // WORKFLOW ENGINE
        displayTopicSet(
            "Workflow engine's topics",
            "Here are the topics used by the workflow engine for this workflow.",
            workflowIsLoading,
            workflowLastUpdated,
            workflowState)

        // WORKFLOW TASK
        displayTopicSet(
            "WorkflowTask's topics",
            "Here are the topics used by the task engine for this workflowTask.",
            workflowTaskIsLoading,
            workflowTaskLastUpdated,
            workflowTaskState)

        // SELECTION SLIDE
        selectionSlide.render(this)
      }

  private fun ElementCreator<Element>.displayTopicSet(
      title: String,
      text: String,
      isLoading: KVar<Boolean>,
      lastUpdated: KVar<Instant>,
      state: KVar<out JobState<out TopicType>>
  ) {
    div().classes("pt-8 pb-8").new {
      div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
        displayJobSectionHeader(title, isLoading, lastUpdated)
        p().classes("mt-7 text-sm text-gray-500").new {
          span().text(text).addText(" Click on a row to get more details on its real-time stats.")
        }
        displayJobStatsTable(
            workflowName, state, selectionSlide, selectionTopicType, selectionTopicStats)
      }
    }
  }
}
