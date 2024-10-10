package io.infinitic.events.listeners

import io.infinitic.common.transport.InfiniticResources
import io.infinitic.common.transport.logged.LoggedInfiniticResources
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.events.EventListener
import io.infinitic.events.config.EventListenerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

context(CoroutineScope)
fun InfiniticResources.checkNewWorkflows(
  config: EventListenerConfig,
  start: suspend (WorkflowName) -> Unit
) = launch {
  val processedWorkflows = mutableSetOf<String>()
  val loggedResources = LoggedInfiniticResources(EventListener.logger, this@checkNewWorkflows)

  while (true) {
    // Retrieve the list of workflows
    loggedResources.getWorkflows().onSuccess { workflows ->
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
