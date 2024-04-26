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

package io.infinitic.pulsar.resources

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedServiceExecutorTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.ServiceTopic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.transport.WorkflowTopic
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.policies.Policies
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk
import io.mockk.spyk
import net.bytebuddy.utility.RandomString

class PulsarResourcesTest : StringSpec(
    {
      val pulsarInfiniticAdmin = mockk<PulsarInfiniticAdmin>()
      val allowedClusters = setOf("foo", "bar")
      val adminRoles = setOf("baz")
      val policies = Policies()
      val tenant = "tenantTest"
      val namespace = "namespaceTest"

      val pulsarResources = PulsarResources(
          pulsarInfiniticAdmin,
          tenant = tenant,
          allowedClusters,
          namespace = namespace,
          adminRoles,
          policies,
      )

      beforeEach {
        clearMocks(pulsarInfiniticAdmin)
      }

      "should delete topic" {
        coEvery { pulsarInfiniticAdmin.deleteTopic(any()) } returns Result.success(Unit)

        val topic = TestFactory.random<String>()
        pulsarResources.deleteTopic(topic)

        coVerifyAll {
          pulsarInfiniticAdmin.deleteTopic(topic)
        }
      }

      "should be able to init delayed topic even if I can not check tenant and namespace" {
        coEvery {
          pulsarInfiniticAdmin.initTenantOnce(any(), any(), any())
        } returns Result.failure(mockk())

        coEvery {
          pulsarInfiniticAdmin.initNamespaceOnce(any(), any())
        } returns Result.failure(mockk())

        coEvery {
          pulsarInfiniticAdmin.initTopicOnce(any(), any(), any(), any())
        } returns Result.success(mockk())

        val topic = TestFactory.random<String>()

        pulsarResources.initTopicOnce(
            topic,
            isPartitioned = true,
            isDelayed = true,
        ).isSuccess shouldBe true

        coVerifyAll {
          pulsarInfiniticAdmin.initTenantOnce("tenantTest", allowedClusters, adminRoles)
          pulsarInfiniticAdmin.initNamespaceOnce("tenantTest/namespaceTest", policies)
          pulsarInfiniticAdmin.initTopicOnce(topic, true, policies.delayedTTLInSeconds)
        }
      }

      "should be able to retrieve workflows name from topics" {
        val workflowName = TestFactory.random<String>()

        for (workflowTopic in WorkflowTopic.entries) {
          val topic = with(pulsarResources) { workflowTopic.fullName(workflowName) }
          val mockResources = spyk(pulsarResources) {
            coEvery { getTopicsFullName() } returns setOf(topic)
          }

          mockResources.getWorkflowsName() shouldBe setOf(workflowName)
        }
      }

      "should be able to retrieve services name from topics" {
        val serviceName = TestFactory.random<String>()

        for (serviceTopic in ServiceTopic.entries) {
          val topic = with(pulsarResources) { serviceTopic.fullName(serviceName) }
          val mockResources = spyk(pulsarResources) {
            coEvery { getTopicsFullName() } returns setOf(topic)
          }

          mockResources.getServicesName() shouldBe setOf(serviceName)
        }
      }

      "topics name MUST not change" {
        val entity = RandomString(10).nextString()
        val prefix = "persistent://$tenant/$namespace"

        with(pulsarResources) {
          ClientTopic.fullName(entity) shouldBe "$prefix/response:$entity"
          WorkflowTagTopic.fullName(entity) shouldBe "$prefix/workflow-tag:$entity"
          WorkflowCmdTopic.fullName(entity) shouldBe "$prefix/workflow-cmd:$entity"
          WorkflowEngineTopic.fullName(entity) shouldBe "$prefix/workflow-engine:$entity"
          DelayedWorkflowEngineTopic.fullName(entity) shouldBe "$prefix/workflow-delay:$entity"
          WorkflowEventsTopic.fullName(entity) shouldBe "$prefix/workflow-events:$entity"
          WorkflowTaskExecutorTopic.fullName(entity) shouldBe "$prefix/workflow-task-executor:$entity"
          DelayedWorkflowTaskExecutorTopic.fullName(entity) shouldBe "$prefix/workflow-task-executor:$entity"
          WorkflowTaskEventsTopic.fullName(entity) shouldBe "$prefix/workflow-task-events:$entity"
          ServiceTagTopic.fullName(entity) shouldBe "$prefix/task-tag:$entity"
          ServiceExecutorTopic.fullName(entity) shouldBe "$prefix/task-executor:$entity"
          DelayedServiceExecutorTopic.fullName(entity) shouldBe "$prefix/task-executor:$entity"
          ServiceEventsTopic.fullName(entity) shouldBe "$prefix/task-events:$entity"
        }
      }
    },
)
