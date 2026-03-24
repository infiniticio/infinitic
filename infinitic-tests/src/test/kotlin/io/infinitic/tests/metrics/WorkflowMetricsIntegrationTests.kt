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
package io.infinitic.tests.metrics

import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.inMemory.resources.InMemoryResources
import io.infinitic.tests.inline.InlineWorkflow
import io.infinitic.transport.config.InMemoryTransportConfig
import io.infinitic.utils.UtilService
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay

internal class WorkflowMetricsIntegrationTests : StringSpec(
    {
      "workflow resolution records consumer metrics with runtime topics" {
        val registry = SimpleMeterRegistry()
        val workerName = "metrics-worker"
        val workflowName = InlineWorkflow::class.java.name
        val serviceName = UtilService::class.java.name

        val config = InfiniticWorkerConfig.fromYamlResource("/pulsar.yml", "/register.yml")
            .copy(
                name = workerName,
                transport = InMemoryTransportConfig(),
            )

        val worker = InfiniticWorker(config).apply { meterRegistry = registry }

        try {
          worker.startAsync()
          
          val client = worker.client
          val workflow = client.newWorkflow(InlineWorkflow::class.java)

          workflow.inline1(7) shouldBe "2 * 7 = 14"

          val resolver = InMemoryResources()
          val workflowCmdTopic = resolver.topicFullName(WorkflowStateCmdTopic, workflowName)
          val workflowEngineTopic = resolver.topicFullName(WorkflowStateEngineTopic, workflowName)
          val workflowExecutorTopic = resolver.topicFullName(WorkflowExecutorTopic, workflowName)
          val serviceExecutorTopic = resolver.topicFullName(ServiceExecutorTopic, serviceName)

          awaitTimerCount(
              registry = registry,
              name = "infinitic.consumer.message.handling",
              workerName = workerName,
              topic = workflowExecutorTopic,
          )
          awaitTimerCount(
              registry = registry,
              name = "infinitic.consumer.message.handling",
              workerName = workerName,
              topic = serviceExecutorTopic,
          )

          registry.timerCount(
              name = "infinitic.consumer.message.handling",
              workerName = workerName,
              topic = workflowCmdTopic,
          ) shouldBeGreaterThan 0L
          registry.timerCount(
              name = "infinitic.consumer.message.handling",
              workerName = workerName,
              topic = workflowEngineTopic,
          ) shouldBeGreaterThan 0L
          registry.timerCount(
              name = "infinitic.consumer.message.handling",
              workerName = workerName,
              topic = workflowExecutorTopic,
          ) shouldBeGreaterThan 0L
          registry.timerCount(
              name = "infinitic.consumer.message.handling",
              workerName = workerName,
              topic = serviceExecutorTopic,
          ) shouldBeGreaterThan 0L

          registry.timerCount(
              name = "infinitic.consumer.message.deserialization",
              workerName = workerName,
              topic = workflowExecutorTopic,
          ) shouldBeGreaterThan 0L
          registry.timerCount(
              name = "infinitic.consumer.message.deserialization",
              workerName = workerName,
              topic = serviceExecutorTopic,
          ) shouldBeGreaterThan 0L

          registry.timerCount(
              name = "infinitic.consumer.message.processing",
              workerName = workerName,
              topic = workflowExecutorTopic,
          ) shouldBeGreaterThan 0L
          registry.timerCount(
              name = "infinitic.consumer.message.processing",
              workerName = workerName,
              topic = serviceExecutorTopic,
          ) shouldBeGreaterThan 0L
        } finally {
          worker.close()
          registry.close()
        }
      }
    },
)

private suspend fun awaitTimerCount(
  registry: SimpleMeterRegistry,
  name: String,
  workerName: String,
  topic: String,
  attempts: Int = 50,
  delayMillis: Long = 100,
): Long {
  repeat(attempts) {
    val count = registry.timerCount(name, workerName, topic)
    if (count > 0L) return count
    delay(delayMillis)
  }

  return registry.timerCount(name, workerName, topic)
}

private fun SimpleMeterRegistry.timerCount(
  name: String,
  workerName: String,
  topic: String,
): Long = meters.asSequence()
    .filter { it.id.name == name }
    .filter { it.id.getTag("worker_name") == workerName }
    .filter { it.id.getTag("topic") == topic }
    .mapNotNull { it as? Timer }
    .sumOf { it.count() }
