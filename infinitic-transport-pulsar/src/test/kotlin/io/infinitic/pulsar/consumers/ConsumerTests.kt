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

package io.infinitic.pulsar.consumers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.fixtures.DockerOnly
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.later
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.pulsar.PulsarInfiniticConsumer
import io.infinitic.pulsar.PulsarInfiniticProducer
import io.infinitic.pulsar.config.pulsarConfigTest
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.bytebuddy.utility.RandomString
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@EnabledIf(DockerOnly::class)
class ConsumerTests : StringSpec(
    {
      val logger = KotlinLogging.logger("test")
      val pulsarConfig = pulsarConfigTest!!
      val resources = pulsarConfig.pulsarResources

      val producer = PulsarInfiniticProducer(
          pulsarConfig.infiniticPulsarClient,
          pulsarConfig.producer,
          resources,
      )

      val consumer = PulsarInfiniticConsumer(
          pulsarConfig.infiniticPulsarClient,
          pulsarConfig.consumer,
          resources,
      )

      afterSpec {
        pulsarConfig.pulsarClient.close()
        pulsarConfig.pulsarAdmin.close()
        DockerOnly().pulsarServer?.stop()
      }

      val zero = MillisDuration(0)

      fun Instant.fromNow() = Duration.between(this, Instant.now()).toMillis().toDouble()

      suspend fun <S : Message> sendMessages(topic: Topic<S>, message: S, total: Int): Double {
        // sending messages
        val start = Instant.now()
        coroutineScope {
          repeat(total) {
            launch {
              @Suppress("UNCHECKED_CAST") val m = when (message) {
                is DispatchWorkflow -> message.copy(workflowId = WorkflowId())
                is ExecuteTask -> message.copy(taskId = TaskId())
                else -> message
              } as S
              with(producer) { m.sendTo(topic, zero) }
            }
          }
        }
        return (start.fromNow() / total).also {
          println("Average time to send a message: $it ms")
        }
      }

      "producing 10000 messages asynchronously should take less than 1 ms in average" {
        val message = TestFactory.random<ExecuteTask>()
        sendMessages(ServiceExecutorTopic, message, 10000) shouldBeLessThan 2.0
      }

      "consuming 1000 messages (1ms) without concurrency should take less than 5 ms in average" {
        with(logger) {
          val entity = RandomString(10).nextString()

          val message = TestFactory.random<ExecuteTask>(
              mapOf("serviceName" to ServiceName(entity)),
          )
          val topic = ServiceExecutorTopic
          val total = 1000

          later {
            // send $total messages
            sendMessages(topic, message, total)
          }

          val subscription = MainSubscription(topic)
          var averageMillisToConsume = 100.0

          val scope = CoroutineScope(Dispatchers.IO)
          val counter = AtomicInteger(0)
          lateinit var start: Instant

          val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit = { _, _ ->
            if (counter.get() == 0) start = Instant.now()
            // emulate a 1ms task
            delay(1)
            // increment counter
            counter.incrementAndGet().let {
              if (it == total) {
                averageMillisToConsume = (start.fromNow() / total)
                println("Average time to consume a message: $averageMillisToConsume ms")
                scope.cancel()
              }
            }
          }

          try {
            with(scope) {
              consumer.start(subscription, entity, null, 1, handler, null)
            }
          } catch (e: CancellationException) {
            // do nothing
          }
          averageMillisToConsume shouldBeLessThan 5.0
        }
      }

      "consuming 1000 messages (100ms) with 100 concurrency should take less than 5 ms in average" {
        with(logger) {
          val entity = RandomString(10).nextString()

          val message = TestFactory.random<ExecuteTask>(
              mapOf("serviceName" to ServiceName(entity)),
          )
          val topic = ServiceExecutorTopic
          val total = 1000

          later {
            // send $total messages
            sendMessages(topic, message, total)
          }

          val subscription = MainSubscription(topic)
          var averageMillisToConsume = 100.0

          val counter = AtomicInteger(0)
          lateinit var start: Instant
          val scope = CoroutineScope(Dispatchers.IO)

          val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit = { _, _ ->
            if (counter.get() == 0) start = Instant.now()
            // emulate a 100ms task
            delay(100)
            // increment counter
            counter.incrementAndGet().let {
              if (it == total) {
                averageMillisToConsume = (start.fromNow() / total)
                println("Average time to consume a message: $averageMillisToConsume ms")
                // delete current scope
                scope.cancel()
              }
            }
          }

          try {
            with(scope) {
              consumer.start(subscription, entity, null, 100, handler, null)
            }
          } catch (e: CancellationException) {
            // do nothing
          }
          averageMillisToConsume shouldBeLessThan 5.0
        }
      }

      "consuming 1000 messages (1ms) with 1 concurrency (key-shared) should take less than 5 ms in average" {
        with(logger) {
          val entity = RandomString(10).nextString()

          val message = TestFactory.random<DispatchWorkflow>(
              mapOf("workflowName" to WorkflowName(entity)),
          )
          val topic = WorkflowStateEngineTopic
          val total = 1000

          // send $total messages
          sendMessages(topic, message, total)

          val subscription = MainSubscription(topic)
          var averageMillisToConsume = 100.0

          val scope = CoroutineScope(Dispatchers.IO)
          val counter = AtomicInteger(0)
          lateinit var start: Instant

          val handler: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit = { _, _ ->
            if (counter.get() == 0) start = Instant.now()
            // emulate a 1ms task
            delay(1)
            // increment counter
            counter.incrementAndGet().let {
              if (it == total) {
                averageMillisToConsume = (start.fromNow() / total)
                println("Average time to consume a message: $averageMillisToConsume ms")
                scope.cancel()
              }
            }
          }

          try {
            with(scope) {
              consumer.start(subscription, entity, null, 1, handler, null)
            }
          } catch (e: CancellationException) {
            // do nothing
          }
          averageMillisToConsume shouldBeLessThan 5.0
        }
      }

      "consuming 1000 messages (100ms) with 100 concurrency (key-shared) should take less than 5 ms in average" {
        with(logger) {
          val entity = RandomString(10).nextString()

          val message = TestFactory.random<DispatchWorkflow>(
              mapOf("workflowName" to WorkflowName(entity)),
          )
          val topic = WorkflowStateEngineTopic
          val total = 1000

          val subscription = MainSubscription(topic)
          var averageMillisToConsume = 100.0

          val scope = CoroutineScope(Dispatchers.IO)
          val counter = AtomicInteger(0)
          lateinit var start: Instant

          val process: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit = { _, _ ->
            if (counter.get() == 0) start = Instant.now()
            // emulate a 100 ms task
            delay(100)
            // increment counter
            counter.incrementAndGet().let {
              if (it == total) {
                averageMillisToConsume = (start.fromNow() / total)
                println("Average time to consume a message: $averageMillisToConsume ms")
                scope.cancel()
              }
            }
          }

          try {
            val job = with(scope) {
              consumer.startAsync(subscription, entity, null, 100, process, null)
            }
            // on the consumer created, we send the messages
            // to avoid that the first consumer up captures all keys right-away
            sendMessages(topic, message, total)
            // wait for the cancellation triggered when reaching total in process
            job.join()
          } catch (e: CancellationException) {
            // do nothing
          }
          averageMillisToConsume shouldBeLessThan 5.0
        }
      }
    },
)
