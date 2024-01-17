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
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.pulsar.client.PulsarInfiniticClient
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.producers.ProducerConfig
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeExactly
import net.bytebuddy.utility.RandomString
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@EnabledIf(DockerOnly::class)
class ConsumerTests : StringSpec(
    {
      val pulsarServer = DockerOnly().pulsarServer!!

      val client = PulsarInfiniticClient(
          PulsarClient.builder().serviceUrl(pulsarServer.pulsarBrokerUrl).build(),
      )
      val producer = Producer(client, ProducerConfig())
      val zero = MillisDuration(0)

      fun Instant.fromNow() = Duration.between(this, Instant.now()).toMillis().toDouble()

      // send `total` messages and returns the average time to send a message in millis
      fun sendMessage(topic: String, total: Int, withKey: Boolean = false): Double {
        // creating messages in advance (creation time of messages should not be into the sending time)
        val message = TestFactory.random<ExecuteTask>()
        val messages = List(total) { message.copy(messageId = MessageId()) }
        // sending messages
        val start = Instant.now()
        val futures = messages.map {
          producer.sendAsync(
              it, zero, topic, "name",
              key = if (withKey) it.messageId.toString() else null,
          )
        }.toTypedArray()
        // wait for all to be sent
        CompletableFuture.allOf(*futures).join()
        return (start.fromNow() / total).also {
          println("Average time to send a message: $it ms")
        }
      }

      fun startAsync(
        consumer: Consumer,
        handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
        topic: String,
        concurrency: Int,
        withKey: Boolean = false
      ) = consumer.runAsync(
          handler = handler,
          beforeDlq = { _, _ -> },
          schemaClass = ServiceExecutorEnvelope::class,
          topic = topic,
          topicDlq = null,
          subscriptionName = topic + "Consumer",
          subscriptionNameDlq = "",
          subscriptionType = if (withKey) SubscriptionType.Key_Shared else SubscriptionType.Shared,
          consumerName = "consumerTest",
          concurrency = concurrency,
      )

      "producing 10000 message in bulk should take less than 1 ms in average" {
        val topic = RandomString(10).nextString()
        sendMessage(topic, 10000).shouldBeLessThan(1.0)
      }

      "consuming 1000 messages (1ms) without concurrency should take less than 5 ms in average" {
        val consumer = Consumer(client, ConsumerConfig())
        val topic = RandomString(10).nextString()
        var averageMillisToConsume = 100.0
        val total = 1000
        val counter = AtomicInteger(0)
        lateinit var start: Instant

        val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit = { _, _ ->
          if (counter.get() == 0) start = Instant.now()
          // emulate a 1ms task
          Thread.sleep(1)
          // increment counter
          counter.incrementAndGet().let {
            if (it == total) {
              averageMillisToConsume = (start.fromNow() / total)
              println("Average time to consume a message: $averageMillisToConsume ms")
              consumer.cancel()
            }
          }
        }

        // start consumers
        val future = startAsync(consumer, handler, topic, 1)

        // send messages
        sendMessage(topic, total)

        // wait for scope cancellation
        try {
          future.join()
        } catch (e: CancellationException) {
          // do nothing
        }

        averageMillisToConsume.shouldBeLessThan(5.0)
      }

      "consuming 1000 messages (100ms) with 100 concurrency (shared) should take less than 5 ms in average" {
        val consumer = Consumer(client, ConsumerConfig())
        val topic = RandomString(10).nextString()
        var averageMillisToConsume = 100.0
        val total = 1000
        val counter = AtomicInteger(0)
        lateinit var start: Instant

        val handler: ((ServiceExecutorMessage, MillisInstant) -> Unit) = { _, _ ->
          if (counter.get() == 0) start = Instant.now()
          // emulate a 100ms task
          Thread.sleep(100)
          // increment counter
          counter.incrementAndGet().let {
            if (it == total) {
              averageMillisToConsume = (start.fromNow() / total)
              println("Average time to consume a message: $averageMillisToConsume ms")
              consumer.cancel()
            }
          }
        }

        // start consumers
        val future = startAsync(consumer, handler, topic, 100)

        // send messages
        sendMessage(topic, total)

        // wait for scope cancellation
        try {
          future.join()
        } catch (e: CancellationException) {
          // do nothing
        }

        averageMillisToConsume.shouldBeLessThan(5.0)
      }

      "consuming 1000 messages (100ms) with 100 concurrency (key-shared) should take less than 5 ms in average" {
        val consumer = Consumer(client, ConsumerConfig())
        val topic = RandomString(10).nextString()
        var averageMillisToConsume = 100.0
        val total = 1000
        val counter = AtomicInteger(0)
        lateinit var start: Instant

        val handler: ((ServiceExecutorMessage, MillisInstant) -> Unit) = { _, _ ->
          if (counter.get() == 0) start = Instant.now()
          // emulate a 100ms task
          Thread.sleep(100)
          // increment counter
          counter.incrementAndGet().let {
            if (it == total) {
              averageMillisToConsume = (start.fromNow() / total)
              println("Average time to consume a message: $averageMillisToConsume ms")
              consumer.cancel()
            }
          }
        }

        // start consumers
        val future = startAsync(consumer, handler, topic, 100, true)

        // send messages
        sendMessage(topic, total, true)

        // wait for scope cancellation
        try {
          future.join()
        } catch (e: CancellationException) {
          // do nothing
        }

        averageMillisToConsume.shouldBeLessThan(5.0)
      }

      "graceful shutdown with Shared" {
        val consumer = Consumer(client, ConsumerConfig())
        val topic = RandomString(10).nextString()
        val counter = AtomicInteger(0)
        val messageOpen = CopyOnWriteArrayList<Int>()
        val messageClosed = CopyOnWriteArrayList<Int>()
        val total = 1000

        val handler: ((ServiceExecutorMessage, MillisInstant) -> Unit) = { _, _ ->
          counter.incrementAndGet().let {
            // begin of task
            messageOpen.add(it)
            // emulate a 100ms task
            Thread.sleep(100)
            // enf of task
            messageClosed.add(it)
          }
        }

        // start consumers
        val future = startAsync(consumer, handler, topic, 100)

        // send messages
        sendMessage(topic, total)

        later(1000) {
          //println("Canceling scope")
          consumer.cancel()
        }

        // wait for scope cancellation
        try {
          future.join()
        } catch (e: CancellationException) {
          // do nothing
        }

        // for the test to be meaningful, all messages should not have been processed
        messageOpen.count().shouldBeLessThan(total)
        messageClosed.count().shouldBeLessThan(total)

        messageOpen.count().shouldBeExactly(messageClosed.count())
      }

      "graceful shutdown with Key-Shared" {
        val consumer = Consumer(client, ConsumerConfig())
        val topic = RandomString(10).nextString()
        val counter = AtomicInteger(0)
        val messageOpen = CopyOnWriteArrayList<Int>()
        val messageClosed = CopyOnWriteArrayList<Int>()
        val total = 1000

        val handler: ((ServiceExecutorMessage, MillisInstant) -> Unit) = { _, _ ->
          counter.incrementAndGet().let {
            // begin of task
            messageOpen.add(it)
            // emulate a 100ms task
            Thread.sleep(100)
            // enf of task
            messageClosed.add(it)
          }
        }

        // start consumers
        val future = startAsync(consumer, handler, topic, 100, true)

        // send messages
        sendMessage(topic, total, true)

        later(1000) {
          //println("Canceling scope")
          consumer.cancel()
        }

        // wait for scope cancellation
        try {
          future.join()
        } catch (e: CancellationException) {
          // do nothing
        }

        // for the test to be meaningful, all messages should not have been processed
        messageOpen.count().shouldBeLessThan(total)
        messageClosed.count().shouldBeLessThan(total)

        messageOpen.count().shouldBeExactly(messageClosed.count())
      }
    },
)
