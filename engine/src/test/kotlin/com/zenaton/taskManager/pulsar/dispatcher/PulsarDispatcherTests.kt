package com.zenaton.taskManager.pulsar.dispatcher

import com.zenaton.commons.utils.TestFactory
import com.zenaton.jobManager.engine.messages.AvroEngineMessage
import com.zenaton.jobManager.metrics.messages.AvroMonitoringPerNameMessage
import com.zenaton.jobManager.workers.AvroWorkerMessage
import com.zenaton.taskManager.engine.CancelJob
import com.zenaton.taskManager.engine.DispatchJob
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.engine.JobAttemptCompleted
import com.zenaton.taskManager.engine.JobAttemptDispatched
import com.zenaton.taskManager.engine.JobAttemptFailed
import com.zenaton.taskManager.engine.JobAttemptStarted
import com.zenaton.taskManager.engine.JobCanceled
import com.zenaton.taskManager.engine.JobCompleted
import com.zenaton.taskManager.engine.JobDispatched
import com.zenaton.taskManager.engine.RetryJob
import com.zenaton.taskManager.engine.RetryJobAttempt
import com.zenaton.taskManager.monitoring.perName.JobStatusUpdated
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.pulsar.Topic
import com.zenaton.taskManager.pulsar.avro.AvroConverter
import com.zenaton.taskManager.workers.RunJob
import com.zenaton.taskManager.workers.WorkerMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.reflect.KClass
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

class PulsarTaskDispatcherTests : StringSpec({
    include(shouldSendTaskMetricMessageToMetricsTopic(JobStatusUpdated::class))

    include(shouldSendTaskWorkerMessageToTaskAttemptsTopic(RunJob::class))

    include(shouldSendTaskEngineMessageToTasksTopic(CancelJob::class))
    include(shouldSendTaskEngineMessageToTasksTopic(DispatchJob::class))
    include(shouldSendTaskEngineMessageToTasksTopic(RetryJob::class))
    include(shouldSendTaskEngineMessageToTasksTopic(RetryJobAttempt::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobAttemptCompleted::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobAttemptDispatched::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobAttemptFailed::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobAttemptStarted::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobCanceled::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobCompleted::class))
    include(shouldSendTaskEngineMessageToTasksTopic(JobDispatched::class))
})

fun <T : EngineMessage> shouldSendTaskEngineMessageToTasksTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroEngineMessage>>()
    val slotSchema = slot<AvroSchema<AvroEngineMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroEngineMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.ENGINE.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroEngineMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.jobId.id) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
fun <T : MonitoringPerNameMessage> shouldSendTaskMetricMessageToMetricsTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroMonitoringPerNameMessage>>()
    val slotSchema = slot<AvroSchema<AvroMonitoringPerNameMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroMonitoringPerNameMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.key(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.MONITORING_PER_NAME.get()
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroMonitoringPerNameMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.key(msg.jobName.name) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}

fun <T : WorkerMessage> shouldSendTaskWorkerMessageToTaskAttemptsTopic(klass: KClass<T>) = stringSpec {
    // mocking
    val context = mockk<Context>()
    val builder = mockk<TypedMessageBuilder<AvroWorkerMessage>>()
    val slotSchema = slot<AvroSchema<AvroWorkerMessage>>()
    val slotTopic = slot<String>()
    every { context.newOutputMessage<AvroWorkerMessage>(capture(slotTopic), capture(slotSchema)) } returns builder
    every { builder.value(any()) } returns builder
    every { builder.send() } returns mockk<MessageId>()
    // given
    val msg = TestFactory.get(klass)
    // when
    PulsarDispatcher(context).dispatch(msg)
    // then
    verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
    slotTopic.captured shouldBe Topic.WORKERS.get(msg.jobName.name)
    slotSchema.captured.avroSchema shouldBe AvroSchema.of(AvroWorkerMessage::class.java).avroSchema
    verify(exactly = 1) { builder.value(AvroConverter.toAvro(msg)) }
    verify(exactly = 1) { builder.send() }
    confirmVerified(context)
    confirmVerified(builder)
}
