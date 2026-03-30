/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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

package io.infinitic.pulsar.schemas

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorEnvelope
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class KSchemaWriterTests : StringSpec(
    {
      "write records producer serialization metrics when registry is configured" {
        val registry = SimpleMeterRegistry()
        val message = TestFactory.random<ExecuteTask>()
        val envelope = ServiceExecutorEnvelope.from(message)
        val writer = KSchemaWriter<ServiceExecutorEnvelope>(
            workerName = "worker-a",
            topic = "persistent://tenant/namespace/service-executor:test",
            registry = registry,
        )

        writer.write(envelope).toList() shouldBe envelope.toByteArray().toList()

        registry.get("infinitic.producer.message.serialization")
            .tags(
                "worker_name",
                "worker-a",
                "topic",
                "persistent://tenant/namespace/service-executor:test",
                "message_type",
                ExecuteTask::class.simpleName!!,
            )
            .timer()
            .count() shouldBe 1L

        registry.find("infinitic.producer.message.serialization.in_flight")
            .tags(
                "worker_name",
                "worker-a",
                "topic",
                "persistent://tenant/namespace/service-executor:test",
                "message_type",
                ExecuteTask::class.simpleName!!,
            )
            .gauge()
            .also { it shouldNotBe null }
            ?.value() shouldBe 0.0
      }

      "write skips metrics when registry context is incomplete" {
        val registry = SimpleMeterRegistry()
        val message = TestFactory.random<ExecuteTask>()
        val envelope = ServiceExecutorEnvelope.from(message)
        val writer = KSchemaWriter<ServiceExecutorEnvelope>(registry = registry)

        writer.write(envelope).toList() shouldBe envelope.toByteArray().toList()

        registry.meters.size shouldBe 0
      }
    },
)
