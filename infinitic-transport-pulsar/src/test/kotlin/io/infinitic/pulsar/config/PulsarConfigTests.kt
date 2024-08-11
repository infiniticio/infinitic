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

package io.infinitic.pulsar.config

import io.infinitic.common.fixtures.DockerOnly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

@EnabledIf(DockerOnly::class)
class PulsarConfigTests : StringSpec(
    {
      val brokerServiceUrl = "pulsar://localhost:6650/"
      val webServiceUrl = "http://localhost:8080"
      val tenant = "infinitic"
      val dev = "dev"

      "Can create PulsarConfig through builder" {
        val config = PulsarConfig.builder()
            .brokerServiceUrl(brokerServiceUrl)
            .webServiceUrl(webServiceUrl)
            .tenant(tenant)
            .namespace(dev)
            .build()
        config shouldBe PulsarConfig(brokerServiceUrl, webServiceUrl, tenant, dev)
      }

      "Create PulsarConfig without brokerServiceUrl should throw" {
        val e = shouldThrow<IllegalArgumentException> {
          PulsarConfig.builder()
              .webServiceUrl(webServiceUrl)
              .tenant(tenant)
              .namespace(dev)
              .build()
        }
        e.message shouldContain "pulsar://localhost:6650"
      }

      "Create PulsarConfig without webServiceUrl should throw" {
        val e = shouldThrow<IllegalArgumentException> {
          PulsarConfig.builder()
              .brokerServiceUrl(brokerServiceUrl)
              .tenant(tenant)
              .namespace(dev)
              .build()
        }
        e.message shouldContain "http://localhost:8080"
      }

      "Create PulsarConfig without tenant should throw" {
        val e = shouldThrow<IllegalArgumentException> {
          PulsarConfig.builder()
              .brokerServiceUrl(brokerServiceUrl)
              .webServiceUrl(webServiceUrl)
              .namespace(dev)
              .build()
        }
        e.message shouldContain "tenant"
      }

      "Create PulsarConfig without namespace should throw" {
        val e = shouldThrow<IllegalArgumentException> {
          PulsarConfig.builder()
              .brokerServiceUrl(brokerServiceUrl)
              .webServiceUrl(webServiceUrl)
              .tenant(tenant)
              .build()
        }
        e.message shouldContain "namespace"
      }
    },
)
