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
package io.infinitic.common.transport

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TopicTests : StringSpec(
    {
      "Checking that topics prefix are not changed" {

        fun Topic<*>.prefix() = when (this) {
          WorkflowTagEngineTopic -> "workflow-tag"
          WorkflowStateCmdTopic -> "workflow-cmd"
          WorkflowStateEngineTopic -> "workflow-engine"
          WorkflowStateTimerTopic -> "workflow-delay"
          WorkflowStateEventTopic -> "workflow-events"
          WorkflowExecutorTopic, RetryWorkflowExecutorTopic -> "workflow-task-executor"
          WorkflowExecutorEventTopic -> "workflow-task-events"
          ServiceTagEngineTopic -> "task-tag"
          ServiceExecutorTopic, RetryServiceExecutorTopic -> "task-executor"
          ServiceExecutorEventTopic -> "task-events"
          ClientTopic -> "response"
          NamingTopic -> "namer"
        }

        with(WorkflowTagEngineTopic) { prefix shouldBe prefix() }
        with(WorkflowStateCmdTopic) { prefix shouldBe prefix() }
        with(WorkflowStateEngineTopic) { prefix shouldBe prefix() }
        with(WorkflowStateTimerTopic) { prefix shouldBe prefix() }
        with(WorkflowStateEventTopic) { prefix shouldBe prefix() }
        with(WorkflowExecutorTopic) { prefix shouldBe prefix() }
        with(RetryWorkflowExecutorTopic) { prefix shouldBe prefix() }
        with(WorkflowExecutorEventTopic) { prefix shouldBe prefix() }
        with(ServiceTagEngineTopic) { prefix shouldBe prefix() }
        with(ServiceExecutorTopic) { prefix shouldBe prefix() }
        with(RetryServiceExecutorTopic) { prefix shouldBe prefix() }
        with(ServiceExecutorEventTopic) { prefix shouldBe prefix() }
        with(ClientTopic) { prefix shouldBe prefix() }
        with(NamingTopic) { prefix shouldBe prefix() }
      }
    },
)

