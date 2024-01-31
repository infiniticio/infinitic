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

package io.infinitic.events.data.workflows
//
//import io.infinitic.common.exceptions.thisShouldNotHappen
//import io.infinitic.common.tasks.data.TaskRetryIndex
//import io.infinitic.common.tasks.data.TaskRetrySequence
//import io.infinitic.common.tasks.executors.messages.ExecuteTask
//import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
//import io.infinitic.common.utils.toJson
//import io.infinitic.events.types.WORKFLOW_EXECUTOR_DISPATCHED
//import kotlinx.serialization.json.JsonObject
//
//fun ServiceExecutorMessage.workflowType(): String? =
//    when (isWorkflowTaskDispatched) {
//      true -> WORKFLOW_EXECUTOR_DISPATCHED
//      false -> null
//    }
//
//fun ServiceExecutorMessage.toWorkflowJson(): JsonObject =
//    when (isWorkflowTaskDispatched) {
//      true -> JsonObject(
//          mapOf(
//              "arg" to (this as ExecuteTask).methodParameters.first().toJson(),
//              "workerName" to emitterName.toJson(),
//              "infiniticVersion" to version.toJson(),
//          ),
//      )
//
//      false -> thisShouldNotHappen()
//    }
//
//
//val ServiceExecutorMessage.isWorkflowTaskDispatched: Boolean
//  get() = (this is ExecuteTask) && this.isWorkflowTask() &&
//      taskRetryIndex == TaskRetryIndex(0) && taskRetrySequence == TaskRetrySequence(0)
