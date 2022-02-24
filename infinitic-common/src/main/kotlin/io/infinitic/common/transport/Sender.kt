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

package io.infinitic.common.transport

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage

interface Sender {

    fun initTask(name: TaskName)

    fun initWorkflow(name: WorkflowName)

    fun sendToClient(message: ClientMessage)

    fun sendToTaskTag(message: TaskTagEngineMessage)

    fun sendToTaskEngine(message: TaskEngineMessage, after: MillisDuration? = MillisDuration(0L))

    fun sendToWorkflowTag(message: WorkflowTagEngineMessage)

    fun sendToWorkflowEngine(message: WorkflowEngineMessage, after: MillisDuration? = MillisDuration(0L))

    fun sendToTaskExecutors(message: TaskExecutorMessage)

    fun sendToMetricsPerName(message: MetricsPerNameMessage)

    fun sendToMetricsGlobal(message: MetricsGlobalMessage)
}
