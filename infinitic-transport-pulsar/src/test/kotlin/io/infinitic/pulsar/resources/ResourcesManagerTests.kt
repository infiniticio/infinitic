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
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.policies.Policies
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk

class ResourcesManagerTests : StringSpec(
    {
      val pulsarInfiniticAdmin = mockk<PulsarInfiniticAdmin>()
      val allowedClusters = setOf("foo", "bar")
      val adminRoles = setOf("baz")
      val policies = Policies()

      val pulsarResources = PulsarResources(
          pulsarInfiniticAdmin,
          "tenantTest",
          allowedClusters,
          "namespaceTest",
          adminRoles,
          policies,
      )

      beforeEach() {
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


    },
)
