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

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.fixtures.DockerOnly
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.fixtures.later
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorEnvelope
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
import kotlinx.coroutines.withContext
import net.bytebuddy.utility.RandomString
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

@EnabledIf(DockerOnly::class)
class ConsumerTests : StringSpec(
    {
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
      val zero = MillisDuration(0)

      fun Instant.fromNow() = Duration.between(this, Instant.now()).toMillis().toDouble()

      suspend fun <S : Message> sendMessages(topic: Topic<S>, message: S, total: Int): Double {
        // sending messages
        val start = Instant.now()
        coroutineScope {
          repeat(total) {
            launch {
              val m = when (message) {
                is DispatchWorkflow -> message.copy(workflowId = WorkflowId())
                is ExecuteTask -> message.copy(taskId = TaskId())
                else -> message
              } as S
              producer.internalSendTo(message = m, topic = topic, after = zero)
            }
          }
        }
        return (start.fromNow() / total).also {
          println("Average time to send a message: $it ms")
        }
      }

      // send `total` messages and returns the average time to send a message in millis
      fun sendMessage(topicName: String, total: Int, withKey: Boolean = false): Double {
        // creating messages in advance (creation time of messages should not be into the sending time)
        val message = TestFactory.random<ExecuteTask>()
        val envelopes = List(total) {
          ServiceExecutorEnvelope.from(message.copy(messageId = MessageId()))
        }
        // sending messages
        val start = Instant.now()
        val futures = envelopes.map {
          producer.sendEnvelopeAsync(
              it, zero, topicName, "name",
              key = if (withKey) it.message().messageId.toString() else null,
          )
        }
        // wait for all to be sent
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        return (start.fromNow() / total).also {
          println("Average time to send a message: $it ms")
        }
      }

      "producing 10000 messages asynchronously should take less than 1 ms in average" {
        val message = TestFactory.random<ExecuteTask>()
        sendMessages(ServiceExecutorTopic, message, 10000) shouldBeLessThan 2.0
      }

      "consuming 1000 messages (1ms) without concurrency should take less than 5 ms in average" {
        val entity = RandomString(10).nextString()
        val message = TestFactory.random<ExecuteTask>(mapOf("serviceName" to ServiceName(entity)))
        val topic = ServiceExecutorTopic
        val total = 1000

        later {
          // send $total messages
          sendMessages(topic, message, total)
        }

        val subscription = MainSubscription(topic)
        var averageMillisToConsume = 100.0

        try {
          withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
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
                  // delete current scope
                  cancel()
                }
              }
            }

            consumer.start(subscription, entity, handler, null, 1)
          }
        } catch (e: CancellationException) {
          // do nothing
        }
        averageMillisToConsume shouldBeLessThan 5.0
      }

      "consuming 1000 messages (100ms) with 100 concurrency should take less than 5 ms in average" {
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

        try {
          withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val counter = AtomicInteger(0)
            lateinit var start: Instant

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
                  cancel()
                }
              }
            }

            consumer.start(subscription, entity, handler, null, 100)
          }
        } catch (e: CancellationException) {
          // do nothing
        }
        averageMillisToConsume shouldBeLessThan 5.0
      }

      "consuming 1000 messages (1ms) with 1 concurrency (key-shared) should take less than 5 ms in average" {
        val entity = RandomString(10).nextString()
        val message = TestFactory.random<DispatchWorkflow>(
            mapOf("workflowName" to WorkflowName(entity)),
        )
        val topic = WorkflowStateEngineTopic
        val total = 1000

        later {
          // send messages
          sendMessages(topic, message, total)
        }

        val subscription = MainSubscription(topic)
        var averageMillisToConsume = 100.0

        try {
          withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
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
                  // delete current scope
                  cancel()
                }
              }
            }

            consumer.start(subscription, entity, handler, null, 1)
          }
        } catch (e: CancellationException) {
          // do nothing
        }

        averageMillisToConsume shouldBeLessThan 5.0
      }

      "consuming 1000 messages (100ms) with 100 concurrency (key-shared) should take less than 5 ms in average" {
        val entity = RandomString(10).nextString()
        val message = TestFactory.random<DispatchWorkflow>(
            mapOf("workflowName" to WorkflowName(entity)),
        )
        val topic = WorkflowStateEngineTopic
        val total = 1000

        later {
          // send messages
          sendMessages(topic, message, total)
        }

        val subscription = MainSubscription(topic)
        var averageMillisToConsume = 100.0

        try {
          withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            val counter = AtomicInteger(0)
            lateinit var start: Instant

            val handler: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit = { _, _ ->
              if (counter.get() == 0) start = Instant.now()
              // emulate a 100ms task
              delay(100)
              // increment counter
              counter.incrementAndGet().let {
                if (it == total) {
                  averageMillisToConsume = (start.fromNow() / total)
                  println("Average time to consume a message: $averageMillisToConsume ms")
                  // delete current scope
                  cancel()
                }
              }
            }

            consumer.start(subscription, entity, handler, null, 100)
          }
        } catch (e: CancellationException) {
          // do nothing
        }

        averageMillisToConsume shouldBeLessThan 5.0
      }

//
//      "graceful shutdown with Shared" {
//        val consumer = ConsumerFactory(client, PulsarConsumerConfig())
//        val topic = RandomString(10).nextString()
//        val counter = AtomicInteger(0)
//        val messageOpen = CopyOnWriteArrayList<Int>()
//        val messageClosed = CopyOnWriteArrayList<Int>()
//        val total = 1000
//
//        val scope = getScope()
//
//        val handler: ((ServiceExecutorMessage, MillisInstant) -> Unit) = { _, _ ->
//          counter.incrementAndGet().let {
//            // begin of task
//            messageOpen.add(it)
//            // emulate a 100ms task
//            Thread.sleep(100)
//            // enf of task
//            messageClosed.add(it)
//          }
//        }
//        // start consumers
//        scope.startAsync(consumer, handler, topic, 100)
//        // send messages
//        sendMessage(topic, total)
//        // cancel after 0.4s
//        later(400) { scope.cancel() }
//        // wait for scope cancellation
//        scope.coroutineContext.job.join()
//
//        // for the test to be meaningful, all messages should not have been processed
//        messageOpen.count().shouldBeLessThan(total)
//        messageClosed.count().shouldBeLessThan(total)
//        messageOpen.count().shouldBeExactly(messageClosed.count())
//      }
//
//      "graceful shutdown with Key-Shared" {
//        val consumer = ConsumerFactory(client, PulsarConsumerConfig())
//        val topic = RandomString(10).nextString()
//        val counter = AtomicInteger(0)
//        val messageOpen = CopyOnWriteArrayList<Int>()
//        val messageClosed = CopyOnWriteArrayList<Int>()
//        val total = 1000
//
//        val scope = getScope()
//
//        val handler: ((ServiceExecutorMessage, MillisInstant) -> Unit) = { _, _ ->
//          counter.incrementAndGet().let {
//            // begin of task
//            messageOpen.add(it)
//            // emulate a 100ms task
//            Thread.sleep(100)
//            // enf of task
//            messageClosed.add(it)
//          }
//        }
//        // start consumers
//        scope.startAsync(consumer, handler, topic, 100, true)
//        // send messages
//        sendMessage(topic, total, true)
//        // cancel after 1s
//        later(1000) { scope.cancel() }
//        // wait for scope cancellation
//        scope.coroutineContext.job.join()
//
//        // for the test to be meaningful, all messages should not have been processed
//        messageOpen.count().shouldBeLessThan(total)
//        messageClosed.count().shouldBeLessThan(total)
//        messageOpen.count().shouldBeExactly(messageClosed.count())
//      }
    },
)
