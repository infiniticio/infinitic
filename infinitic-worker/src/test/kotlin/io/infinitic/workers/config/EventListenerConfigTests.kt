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
import io.infinitic.cloudEvents.EntityListConfig
import io.infinitic.common.utils.annotatedName
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.WorkflowA
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class TestEventListener : CloudEventListener {
  override fun onEvents(cloudEvents: CloudEvent) {}
}

internal class EventListenerConfigTests : StringSpec(
    {
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
        config.serviceListConfig shouldBe EntityListConfig(60.0, null, listOf())
        config.workflowListConfig shouldBe EntityListConfig(60.0, null, listOf())
      }

      "Can create EventListenerConfig through Yaml with default parameters" {
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
        config.serviceListConfig shouldBe EntityListConfig(60.0, null, listOf())
        config.workflowListConfig shouldBe EntityListConfig(60.0, null, listOf())
      }

      "Can create EventListenerConfig through builder with all parameters" {
        val config = shouldNotThrowAny {
          EventListenerConfig.builder()
              .setListener(listener)
              .setConcurrency(10)
              .setSubscriptionName("subscriptionName")
              .setServiceListRefreshSeconds(10.0)
              .setWorkflowListRefreshSeconds(20.0)
              .allowServices("service1", "service2")
              .allowServices("service3")
              .allowServices(ServiceA::class.java)
              .allowWorkflows("workflow1", "workflow2")
              .allowWorkflows("workflow3")
              .allowWorkflows(WorkflowA::class.java)
              .disallowServices("service4", "service5")
              .disallowServices("service6")
              .disallowServices(ServiceA::class.java)
              .disallowWorkflows("workflow4", "workflow5")
              .disallowWorkflows("workflow6")
              .disallowWorkflows(WorkflowA::class.java)
              .build()
        }

        config.shouldBeInstanceOf<EventListenerConfig>()
        config.listener shouldBe listener
        config.concurrency shouldBe 10
        config.subscriptionName shouldBe "subscriptionName"
        config.serviceListConfig shouldBe EntityListConfig(
            10.0,
            listOf("service1", "service2", "service3", ServiceA::class.java.annotatedName),
            listOf("service4", "service5", "service6", ServiceA::class.java.annotatedName),
        )
        config.workflowListConfig shouldBe EntityListConfig(
            20.0,
            listOf("workflow1", "workflow2", "workflow3", WorkflowA::class.java.annotatedName),
            listOf("workflow4", "workflow5", "workflow6", WorkflowA::class.java.annotatedName),
        )
      }

      "Can create EventListenerConfig through YMAL with all parameters" {
        val config = shouldNotThrowAny {
          EventListenerConfig.fromYamlString(
              """
class: ${TestEventListener::class.java.name}
concurrency: 10
subscriptionName: subscriptionName
refreshDelaySeconds: 10
services:
  listRefreshSeconds: 10
  allow:
    - service1
    - service2
    - service3
  disallow:
    - service4
    - service5
    - service6
workflows:
  listRefreshSeconds: 20
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
        config.serviceListConfig shouldBe EntityListConfig(
            10.0,
            listOf("service1", "service2", "service3"),
            listOf("service4", "service5", "service6"),
        )
        config.workflowListConfig shouldBe EntityListConfig(
            20.0,
            listOf("workflow1", "workflow2", "workflow3"),
            listOf("workflow4", "workflow5", "workflow6"),
        )
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
