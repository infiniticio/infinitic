/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
package io.infinitic.common.workflows.engine.state

import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.steps.StepStatus

/**
 * Represents the current status of a workflow.
 * This is a convenience enum extracted from WorkflowState for querying purposes.
 */
enum class WorkflowStatus {
  /** Workflow is currently running */
  RUNNING,

  /** One or more tasks have failed */
  HAS_FAILED_TASK,

  /** The workflow task itself has failed */
  HAS_FAILED_EXECUTOR,

  /** Workflow is waiting for external events (signals, timers, etc.) */
  WAITING,

  /** Workflow has completed successfully */
  COMPLETED;

  companion object {
    /**
     * Extracts the workflow status from a WorkflowState.
     *
     * Algorithm:
     * - If workflow task has failed => WORKFLOW_FAILED
     * - Else if any task has failed => TASK_FAILED
     * - Else if waiting (no running workflow task but has methods) => WAITING
     * - Else if running (has running workflow task) => RUNNING
     * - Else => COMPLETED
     */
    fun from(workflowState: WorkflowState): WorkflowStatus {
      // Check if workflow task has failed
      // A workflow task failure would be indicated by buffered messages that couldn't be processed
      // or by checking if there's no running workflow task but the workflow is not completed
      val hasWorkflowMethods = workflowState.workflowMethods.isNotEmpty()
      val hasRunningWorkflowTask = workflowState.runningWorkflowTaskId != null

      // Check if any task has failed
      val hasFailedTask = workflowState.workflowMethods.any { method ->
        method.pastCommands.any { pastCommand ->
          pastCommand.commandStatus !is CommandStatus.Ongoing &&
              pastCommand.commandStatus !is CommandStatus.Completed
        }
      }

      workflowState.runningWorkflowTaskId != null

      workflowState.workflowMethods.any {
        it.currentStep?.stepStatus == StepStatus.Waiting
      }

      // Determine if the workflow task has failed
      // This is indicated by having methods but no running workflow task and not all methods are terminated
      val hasWorkflowTaskFailed = hasWorkflowMethods &&
          !hasRunningWorkflowTask &&
          hasFailedTask &&
          workflowState.workflowMethods.any { !it.isTerminated() }

      return when {
        hasWorkflowTaskFailed -> HAS_FAILED_EXECUTOR
        hasFailedTask -> HAS_FAILED_TASK
        !hasRunningWorkflowTask && hasWorkflowMethods -> WAITING
        hasRunningWorkflowTask -> RUNNING
        else -> COMPLETED
      }
    }
  }
}
