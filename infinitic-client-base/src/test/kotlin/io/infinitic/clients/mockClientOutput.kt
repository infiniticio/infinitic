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
package io.infinitic.clients

import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future

fun mockSendToWorkflowTagEngine(
    client: InfiniticClientInterface,
    workflowTagSlots: CopyOnWriteArrayList<WorkflowTagMessage>,
    clientName: ClientName,
    sendingScope: CoroutineScope
): SendToWorkflowTag {
  val sendToWorkflowTagEngine = mockk<SendToWorkflowTag>()
  every { sendToWorkflowTagEngine(capture(workflowTagSlots)) } answers
      {
        workflowTagSlots.forEach {
          if (it is GetWorkflowIdsByTag) {
            val workflowIdsByTag =
                WorkflowIdsByTag(
                    recipientName = clientName,
                    workflowName = it.workflowName,
                    workflowTag = it.workflowTag,
                    workflowIds = setOf(WorkflowId(), WorkflowId()),
                    emitterName = ClientName("mockk"))
            sendingScope.future {
              delay(100)
              client.handle(workflowIdsByTag)
            }
          }
        }
      }
  return sendToWorkflowTagEngine
}

fun mockSendToWorkflowEngine(
    client: InfiniticClientInterface,
    message: CapturingSlot<WorkflowEngineMessage>,
    clientName: ClientName,
    sendingScope: CoroutineScope
): SendToWorkflowEngine {
  val sendToWorkflowEngine = mockk<SendToWorkflowEngine>()
  every { sendToWorkflowEngine(capture(message)) } answers
      {
        val msg: WorkflowEngineMessage = message.captured
        if (msg is DispatchWorkflow && msg.clientWaiting || msg is WaitWorkflow) {
          val workflowCompleted =
              MethodCompleted(
                  recipientName = clientName,
                  workflowId = msg.workflowId,
                  methodRunId = MethodRunId.from(msg.workflowId),
                  methodReturnValue = ReturnValue.from("success"),
                  emitterName = ClientName("mockk"))
          sendingScope.future {
            delay(100)
            client.handle(workflowCompleted)
          }
        }
      }

  return sendToWorkflowEngine
}
