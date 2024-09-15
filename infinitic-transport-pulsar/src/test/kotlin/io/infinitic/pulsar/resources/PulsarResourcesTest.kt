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
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.ServiceTopic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorRetryTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.WorkflowTopic
import io.infinitic.pulsar.admin.InfiniticPulsarAdmin
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import net.bytebuddy.utility.RandomString

class PulsarResourcesTest : StringSpec(
    {
      val mockedAdmin = mockk<InfiniticPulsarAdmin>()

      val allowedClusters = setOf("foo", "bar")
      val adminRoles = setOf("baz")
      val policiesConfig = PoliciesConfig()
      val tenant = "tenantTest"
      val namespace = "namespaceTest"

      val pulsarConfig = PulsarConfig(
          brokerServiceUrl = "pulsar://localhost:6650",
          webServiceUrl = "http://localhost:8080",
          tenant = tenant,
          namespace = namespace,
          allowedClusters = allowedClusters,
          adminRoles = adminRoles,
          policies = policiesConfig,
      )

      val pulsarResources = spyk(PulsarResources(pulsarConfig)) {
        every { admin } returns mockedAdmin
      }

      beforeEach {
        clearMocks(mockedAdmin)
      }

      "should delete topic" {
        coEvery { mockedAdmin.deleteTopic(any()) } returns Result.success(Unit)
        val topic = TestFactory.random<String>()
        pulsarResources.deleteTopic(topic)

        coVerifyAll {
          mockedAdmin.deleteTopic(topic)
        }
      }

      "should be able to init delayed topic even if I can not check tenant and namespace" {
        coEvery {
          mockedAdmin.syncInitTenantOnce(any(), any(), any())
        } returns Result.failure(mockk())

        coEvery {
          mockedAdmin.syncInitNamespaceOnce(any(), any())
        } returns Result.failure(mockk())

        coEvery {
          mockedAdmin.syncInitTopicOnce(any(), any(), any())
        } returns Result.success(mockk())

        val topic = TestFactory.random<String>()

        pulsarResources.initTopicOnce(
            topic,
            isPartitioned = true,
            isTimer = true,
        ).isSuccess shouldBe true

        coVerifyAll {
          mockedAdmin.syncInitTenantOnce("tenantTest", allowedClusters, adminRoles)
          mockedAdmin.syncInitNamespaceOnce("tenantTest/namespaceTest", policiesConfig)
          mockedAdmin.syncInitTopicOnce(topic, true, policiesConfig.timerTTLSeconds)
        }
      }

      "should be able to retrieve workflows name from topics" {
        val workflowName = TestFactory.random<String>()

        for (workflowTopic in WorkflowTopic.entries) {
          val topic = with(pulsarResources) { workflowTopic.fullName(workflowName) }
          val mockResources = spyk(pulsarResources) {
            coEvery { getTopicsFullName() } returns Result.success(setOf(topic))
          }

          mockResources.getWorkflowNames().getOrThrow() shouldBe setOf(workflowName)
        }
      }

      "should be able to retrieve services name from topics" {
        val serviceName = TestFactory.random<String>()

        for (serviceTopic in ServiceTopic.entries) {
          val topic = with(pulsarResources) { serviceTopic.fullName(serviceName) }
          val mockResources = spyk(pulsarResources) {
            coEvery { getTopicsFullName() } returns Result.success(setOf(topic))
          }

          mockResources.getServiceNames().getOrThrow() shouldBe setOf(serviceName)
        }
      }

      "topics name MUST not change" {
        val entity = RandomString(10).nextString()
        val domain = "persistent://$tenant/$namespace"

        with(pulsarResources) {
          ClientTopic.fullName(entity) shouldBe "$domain/response:$entity"
          WorkflowTagEngineTopic.fullName(entity) shouldBe "$domain/workflow-tag:$entity"
          WorkflowStateCmdTopic.fullName(entity) shouldBe "$domain/workflow-cmd:$entity"
          WorkflowStateEngineTopic.fullName(entity) shouldBe "$domain/workflow-engine:$entity"
          WorkflowStateTimerTopic.fullName(entity) shouldBe "$domain/workflow-delay:$entity"
          WorkflowStateEventTopic.fullName(entity) shouldBe "$domain/workflow-events:$entity"
          WorkflowExecutorTopic.fullName(entity) shouldBe "$domain/workflow-task-executor:$entity"
          WorkflowExecutorRetryTopic.fullName(entity) shouldBe "$domain/workflow-task-retry:$entity"
          WorkflowExecutorEventTopic.fullName(entity) shouldBe "$domain/workflow-task-events:$entity"
          ServiceTagEngineTopic.fullName(entity) shouldBe "$domain/task-tag:$entity"
          ServiceExecutorTopic.fullName(entity) shouldBe "$domain/task-executor:$entity"
          ServiceExecutorRetryTopic.fullName(entity) shouldBe "$domain/task-retry:$entity"
          ServiceExecutorEventTopic.fullName(entity) shouldBe "$domain/task-events:$entity"
        }
      }
    },
)
