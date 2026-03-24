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
package io.infinitic.inMemory.resources

import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InMemoryResourcesTest : StringSpec(
    {
      val resources = InMemoryResources()

      "resolver should produce stable in-memory topic names" {
        resources.topicName(ServiceExecutorTopic, "MyService") shouldBe "task-executor:MyService"
        resources.topicFullName(ServiceExecutorTopic, "MyService") shouldBe
            "inmemory://task-executor:MyService"
        resources.topicName(WorkflowStateEngineTopic, null) shouldBe "workflow-engine"
        resources.topicFullName(WorkflowStateEngineTopic, null) shouldBe
            "inmemory://workflow-engine"
        resources.topicDlqName(ServiceExecutorTopic, "MyService") shouldBe
            "task-executor-dlq:MyService"
        resources.topicDlqFullName(ServiceExecutorTopic, "MyService") shouldBe
            "inmemory://task-executor-dlq:MyService"
      }
    },
)
