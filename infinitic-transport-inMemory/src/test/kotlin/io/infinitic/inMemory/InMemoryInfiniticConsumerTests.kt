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
package io.infinitic.inMemory

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.consumers.startProcessingWithoutKey
import io.infinitic.inMemory.channels.InMemoryChannels
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class InMemoryInfiniticConsumerTests : StringSpec(
    {
      val logger = KotlinLogging.logger {}
      val serviceName = ServiceName("ServiceTest")
      val mainChannels = InMemoryChannels()
      val eventListenerChannels = InMemoryChannels()

      val consumerFactory = InMemoryConsumerFactory(mainChannels, eventListenerChannels)
      val producer = InMemoryInfiniticProducer(mainChannels, eventListenerChannels)

      val counter = AtomicInteger(0)

      fun process(msg: ServiceExecutorMessage, publishTime: MillisInstant) {
        println(counter.incrementAndGet())
        Thread.sleep(1000)
      }

      val executeTask = TestFactory.random<ExecuteTask>().copy(serviceName = serviceName)

      "Tasks should be processed in parallel" {
        val scope = CoroutineScope(Dispatchers.IO)

        repeat(10) {
          val m = executeTask.copy(taskId = TaskId())
          with(producer) { m.sendTo(ServiceExecutorTopic) }
        }

        val consumer = consumerFactory.newConsumer(
            MainSubscription(ServiceExecutorTopic),
            serviceName.toString(),
            null,
        )

        val duration = measureTimeMillis {
          scope.launch {
            coroutineScope {
              startProcessingWithoutKey(
                  logger = logger,
                  consumer = consumer,
                  concurrency = 10,
                  processor = ::process,
              )
              // we wait a bit to make sure that tasks have started
              delay(200)
              cancel()
            }
          }.join()
        }
        duration shouldBeLessThan 1200L
        counter.get() shouldBe 10
      }
    },
)
