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
package io.infinitic.events.listeners

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.transport.interfaces.InfiniticResources
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.events.config.EventListenerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

context(CoroutineScope, KLogger)
internal fun InfiniticResources.refreshWorkflowListAsync(
  config: EventListenerConfig,
  start: suspend (WorkflowName) -> Unit
) = launch {
  val processedWorkflows = mutableSetOf<String>()

  while (true) {
    // Retrieve the list of workflows
    getWorkflows().onSuccess { workflows ->
      val currentWorkflows = workflows.filter { config.includeWorkflow(it) }

      // Determine new workflows that haven't been processed
      val newWorkflows = currentWorkflows.filterNot { it in processedWorkflows }

      // Launch starter for each new workflow
      for (workflow in newWorkflows) {
        start(WorkflowName(workflow))
        // Add the workflow to the set of processed workflows
        processedWorkflows.add(workflow)
      }
    }

    delay(config.workflowListConfig.listRefreshMillis)
  }
}
