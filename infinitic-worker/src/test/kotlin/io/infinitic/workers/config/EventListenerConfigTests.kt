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
package io.infinitic.workers.config

import io.cloudevents.CloudEvent
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.workers.samples.ServiceAImpl
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class EventListenerConfigTests : StringSpec(
    {
      class TestEventListener : CloudEventListener {
        override fun onEvent(event: CloudEvent) {}
      }

      val listener = TestEventListener()

      "Can create EventListenerConfig through builder with default parameters" {
        val config = shouldNotThrowAny {
          EventListenerConfig.builder()
              .setListener(listener)
              .build()
        }

        config.shouldBeInstanceOf<EventListenerConfig>()
        config.listener shouldBe listener
        config.concurrency shouldBe 1
        config.subscriptionName shouldBe null
        config.allowedServices shouldBe null
        config.allowedWorkflows shouldBe null
        config.disallowedServices.size shouldBe 0
        config.disallowedWorkflows.size shouldBe 0
      }

      "Can create EventListenerConfig through YMAL with default parameters" {
        val config = shouldNotThrowAny {
          EventListenerConfig.fromYamlString(
              """
class: ${TestEventListener::class.java.name}
          """,
          )
        }

        config.shouldBeInstanceOf<EventListenerConfig>()
        config.listener::class shouldBe TestEventListener::class
        config.concurrency shouldBe 1
        config.subscriptionName shouldBe null
        config.allowedServices shouldBe null
        config.allowedWorkflows shouldBe null
        config.disallowedServices.size shouldBe 0
        config.disallowedWorkflows.size shouldBe 0
      }

      "Can create EventListenerConfig through builder with all parameters" {
        val config = shouldNotThrowAny {
          EventListenerConfig.builder()
              .setListener(listener)
              .setConcurrency(10)
              .setSubscriptionName("subscriptionName")
              .allowServices("service1", "service2")
              .allowServices("service3")
              .allowWorkflows("workflow1", "workflow2")
              .allowWorkflows("workflow3")
              .disallowServices("service4", "service5")
              .disallowServices("service6")
              .disallowWorkflows("workflow4", "workflow5")
              .disallowWorkflows("workflow6")
              .build()
        }

        config.shouldBeInstanceOf<EventListenerConfig>()
        config.listener shouldBe listener
        config.concurrency shouldBe 10
        config.subscriptionName shouldBe "subscriptionName"
        config.allowedServices shouldBe listOf("service1", "service2", "service3")
        config.allowedWorkflows shouldBe listOf("workflow1", "workflow2", "workflow3")
        config.disallowedServices shouldBe listOf("service4", "service5", "service6")
        config.disallowedWorkflows shouldBe listOf("workflow4", "workflow5", "workflow6")
      }

      "Can create EventListenerConfig through YMAL with all parameters" {
        val config = shouldNotThrowAny {
          EventListenerConfig.fromYamlString(
              """
class: ${TestEventListener::class.java.name}
concurrency: 10
subscriptionName: subscriptionName
services:
  allow:
    - service1
    - service2
    - service3
  disallow:
    - service4
    - service5
    - service6
workflows:
  allow:
    - workflow1
    - workflow2
    - workflow3
  disallow:
    - workflow4
    - workflow5
    - workflow6
          """,
          )
        }
        config.shouldBeInstanceOf<EventListenerConfig>()
        config.listener::class shouldBe TestEventListener::class
        config.concurrency shouldBe 10
        config.subscriptionName shouldBe "subscriptionName"
        config.allowedServices shouldBe listOf("service1", "service2", "service3")
        config.allowedWorkflows shouldBe listOf("workflow1", "workflow2", "workflow3")
        config.disallowedServices shouldBe listOf("service4", "service5", "service6")
        config.disallowedWorkflows shouldBe listOf("workflow4", "workflow5", "workflow6")
      }

      "Listener not implementing CloudEventListener should throw exception" {
        val e = shouldThrowAny {
          EventListenerConfig.fromYamlString(
              """
class: ${ServiceAImpl::class.java.name}
          """,
          )
        }
        e.message shouldContain "CloudEventListener"
      }

      "Listener not found should throw exception" {
        val e = shouldThrowAny {
          EventListenerConfig.fromYamlString(
              """
class: UnknownClass
          """,
          )
        }
        e.message shouldContain "Class 'UnknownClass' not found"
      }
    },
)
