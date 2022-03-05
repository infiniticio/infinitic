/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar

// class PulsarTests : StringSpec({
//    WorkflowEngineMessage::class.sealedSubclasses.forEach {
//        include(shouldBeAbleToSendMessageToWorkflowEngineCommandsTopic(TestFactory.random(it)))
//    }
//
//    TaskEngineMessage::class.sealedSubclasses.forEach {
//        include(shouldBeAbleToSendMessageToTaskEngineCommandsTopic(TestFactory.random(it)))
//    }
//
// //    MetricsPerNameMessage::class.sealedSubclasses.forEach {
// //        include(shouldBeAbleToSendMessageToMetricsPerNameTopic(TestFactory.random(it)))
// //    }
//
//    GlobalMetricsMessage::class.sealedSubclasses.forEach {
//        include(shouldBeAbleToSendMessageToMetricsGlobalTopic(TestFactory.random(it)))
//    }
//
//    TaskExecutorMessage::class.sealedSubclasses.forEach {
//        include(shouldBeAbleToSendMessageToTaskExecutorTopic(TestFactory.random(it)))
//    }
// })
//
// private fun shouldBeAbleToSendMessageToWorkflowEngineCommandsTopic(msg: WorkflowEngineMessage) = stringSpec {
//    "${msg::class.simpleName!!} can be send to WorkflowEngineCommands topic" {
//        // given
//        val context = context()
//        val builder = mockk<TypedMessageBuilder<WorkflowEngineEnvelope>>()
//        val slotSchema = slot<AvroSchema<WorkflowEngineEnvelope>>()
//        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
//        every { builder.value(any()) } returns builder
//        every { builder.key(any()) } returns builder
//        every { builder.send() } returns mockk()
//        // when
//        PulsarOutput.from(context).sendToWorkflowEngine()(msg)
//        // then
//        verify {
//            context.newOutputMessage(
//                "persistent://tenant/namespace/workflow-engine: ${msg.workflowName}",
//                slotSchema.captured
//            )
//        }
//        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<WorkflowEngineEnvelope>()).avroSchema
//        verifyAll {
//            builder.value(WorkflowEngineEnvelope.from(msg))
//            builder.key("${msg.workflowId}")
//            builder.send()
//        }
//        confirmVerified(builder)
//    }
// }
//
// private fun shouldBeAbleToSendMessageToTaskEngineCommandsTopic(msg: TaskEngineMessage) = stringSpec {
//    "${msg::class.simpleName!!} can be send to TaskEngineCommands topic" {
//        // given
//        val context = context()
//        val builder = mockk<TypedMessageBuilder<TaskEngineEnvelope>>()
//        val slotSchema = slot<AvroSchema<TaskEngineEnvelope>>()
//        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
//        every { builder.value(any()) } returns builder
//        every { builder.key(any()) } returns builder
//        every { builder.send() } returns mockk()
//        // when
//        PulsarOutput.from(context).sendToTaskEngine()(msg)
//        // then
//        verify {
//            context.newOutputMessage(
//                "persistent://tenant/namespace/task-engine: ${msg.taskName}",
//                slotSchema.captured
//            )
//        }
//        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskEngineEnvelope>()).avroSchema
//        verifyAll {
//            builder.value(TaskEngineEnvelope.from(msg))
//            builder.key("${msg.taskId}")
//            builder.send()
//        }
//        confirmVerified(builder)
//    }
// }
//
// // private fun shouldBeAbleToSendMessageToMetricsPerNameTopic(msg: MetricsPerNameMessage) = stringSpec {
// //    "${msg::class.simpleName!!} can be send to MetricsPerName topic " {
// //        // given
// //        val context = context()
// //        val builder = mockk<TypedMessageBuilder<MetricsPerNameEnvelope>>()
// //        val slotSchema = slot<AvroSchema<MetricsPerNameEnvelope>>()
// //        every { context.newOutputMessage(any(), capture(slotSchema)) } returns builder
// //        every { builder.value(any()) } returns builder
// //        every { builder.key(any()) } returns builder
// //        every { builder.send() } returns mockk()
// //        // when
// //        PulsarOutput.from(context).sendToMetricsPerName()(msg)
// //        // then
// //        verify {
// //            context.newOutputMessage(
// //                "persistent://tenant/namespace/task-metrics: ${msg.taskName}",
// //                slotSchema.captured
// //            )
// //        }
// //        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<MetricsPerNameEnvelope>()).avroSchema
// //        verifyAll {
// //            builder.value(MetricsPerNameEnvelope.from(msg))
// //            builder.send()
// //        }
// //        confirmVerified(builder)
// //    }
// // }
//
// private fun shouldBeAbleToSendMessageToMetricsGlobalTopic(msg: GlobalMetricsMessage) = stringSpec {
//    "${msg::class.simpleName!!} can be send to MetricsGlobal topic " {
//        // given
//        val context = context()
//        val builder = mockk<TypedMessageBuilder<GlobalMetricsEnvelope>>()
//        val slotSchema = slot<AvroSchema<GlobalMetricsEnvelope>>()
//        val slotTopic = slot<String>()
//        every { context.newOutputMessage(capture(slotTopic), capture(slotSchema)) } returns builder
//        every { builder.value(any()) } returns builder
//        every { builder.key(any()) } returns builder
//        every { builder.send() } returns mockk()
//        // when
//        PulsarOutput.from(context).sendToGlobalMetrics()(msg)
//        // then
//        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
//        slotTopic.captured shouldBe "persistent://tenant/namespace/global-metrics"
//        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<GlobalMetricsEnvelope>()).avroSchema
//        verify(exactly = 1) { builder.value(GlobalMetricsEnvelope.from(msg)) }
//        verify(exactly = 1) { builder.send() }
//        confirmVerified(builder)
//    }
// }
//
// private fun shouldBeAbleToSendMessageToTaskExecutorTopic(msg: TaskExecutorMessage) = stringSpec {
//    "${msg::class.simpleName!!} can be send to TaskExecutor topic" {
//        val context = context()
//        val builder = mockk<TypedMessageBuilder<TaskExecutorEnvelope>>()
//        val slotSchema = slot<AvroSchema<TaskExecutorEnvelope>>()
//        val slotTopic = slot<String>()
//        every { context.newOutputMessage(capture(slotTopic), capture(slotSchema)) } returns builder
//        every { builder.value(any()) } returns builder
//        every { builder.key(any()) } returns builder
//        every { builder.send() } returns mockk()
//        // when
//        PulsarOutput.from(context).sendToTaskExecutors()(msg)
//        // then
//        verify(exactly = 1) { context.newOutputMessage(slotTopic.captured, slotSchema.captured) }
//        slotTopic.captured shouldBe "persistent://tenant/namespace/task-executors: ${msg.taskName}"
//        slotSchema.captured.avroSchema shouldBe AvroSchema.of(schemaDefinition<TaskExecutorEnvelope>()).avroSchema
//        verify(exactly = 1) { builder.value(TaskExecutorEnvelope.from(msg)) }
//        verify(exactly = 1) { builder.send() }
//        confirmVerified(builder)
//    }
// }
//
// fun context(): Context {
//    val context = mockk<Context>()
//    every { context.logger } returns mockk(relaxed = true)
//    every { context.tenant } returns "tenant"
//    every { context.namespace } returns "namespace"
//
//    return context
// }