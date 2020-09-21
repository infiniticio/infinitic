package io.infinitic.workflowManager.engine.avroConverter

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.data.commands.CommandStatus
import io.infinitic.workflowManager.common.data.steps.Step
import io.infinitic.workflowManager.common.data.steps.StepStatus
import io.infinitic.workflowManager.common.utils.TestFactory
import io.infinitic.workflowManager.data.commands.AvroCommandStatus
import io.infinitic.workflowManager.data.steps.AvroStep
import io.infinitic.workflowManager.data.steps.AvroStepStatus
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class AvroConverterTests : ShouldSpec({
    context("CommandStatus") {
        CommandStatus::class.sealedSubclasses.forEach {
            should("${it.simpleName} be avro-reversible") {
                val obj1 = TestFactory.random(it)
                val avro1 = AvroConverter.convertJson<AvroCommandStatus>(obj1)
                val obj2 = AvroConverter.convertJson<CommandStatus>(avro1)
                val avro2 = AvroConverter.convertJson<AvroCommandStatus>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
    }

    context("StepStatus") {
        StepStatus::class.sealedSubclasses.forEach {
            should("${it.simpleName} be avro-reversible") {
                val obj1 = TestFactory.random(it)
                val avro1 = AvroConverter.convertJson<AvroStepStatus>(obj1)
                val obj2 = AvroConverter.convertJson<StepStatus>(avro1)
                val avro2 = AvroConverter.convertJson<AvroStepStatus>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
    }

    context("Step") {
        TestFactory.steps().forEach {
            val obj1 = it.value
            should("${it.key} be avro-convertible") {
                val avro1 = AvroConverter.convertJson<AvroStep>(obj1)
                val obj2 = AvroConverter.convertJson<Step>(avro1)
                val avro2 = AvroConverter.convertJson<AvroStep>(obj2)

                obj1 shouldBe obj2
                avro1 shouldBe avro2
            }
        }
    }

//    context("WorkflowState") {
//        should("should be avro-reversible") {
//            val o1 = TestFactory.random(WorkflowState::class)
//            val o2 = AvroConverter.toStorage(o1)
//            val o3 = AvroConverter.fromStorage(o2)
//            val o4 = AvroConverter.toStorage(o3)
//            o1 shouldBe o3
//            o2 shouldBe o4
//        }
//    }
//
//    context("DecisionInput") {
//        should("should be avro-reversible") {
//            val o1 = TestFactory.random(DecisionInput::class)
//            val o2 = AvroConverter.toAvroDecisionInput(o1)
//            val o3 = AvroConverter.fromAvroDecisionInput(o2)
//            val o4 = AvroConverter.toAvroDecisionInput(o3)
//            o1 shouldBe o3
//            o2 shouldBe o4
//        }
//    }
//
//    context("Commands") {
//        should("should all be avro-reversible") {
//            fun checkCommand(klass: KClass<out Command>) {
//                if (klass.isSealed) {
//                    klass.sealedSubclasses.forEach { checkCommand(it) }
//                } else {
//                    val o1 = TestFactory.random(klass)
//                    val o2 = AvroConverter.convertJson<AvroCommand>(o1)
//                    val o3 = AvroConverter.convertJson<Command>(o2)
//                    val o4 = AvroConverter.convertJson<AvroCommand>(o3)
//                    o1 shouldBe o3
//                    o2 shouldBe o4
//                }
//            }
//            checkCommand(Command::class)
//        }
//    }
//
//    context("Branch") {
//        should("should be avro-reversible") {
//            val o1 = TestFactory.random(Branch::class)
//            val o2 = AvroConverter.toAvroBranch(o1)
//            val o3 = AvroConverter.fromAvroBranch(o2)
//            val o4 = AvroConverter.toAvroBranch(o3)
//            o1 shouldBe o3
//            o2 shouldBe o4
//        }
//    }
//
//
//
//    ForWorkflowEngineMessage::class.sealedSubclasses.forEach {
//        val msg = TestFactory.random(it)
//        include(messagesToWorkflowEngineShouldBeAvroReversible(msg))
//    }
})

// fun messagesToWorkflowEngineShouldBeAvroReversible(msg: ForWorkflowEngineMessage) = shouldSpec {
//    context(msg::class.simpleName!!) {
//        should("should be avro-convertible") {
//            shouldNotThrowAny {
//                val avroMsg = AvroConverter.toWorkflowEngine(msg)
//                val msg2 = AvroConverter.fromWorkflowEngine(avroMsg)
//                val avroMsg2 = AvroConverter.toWorkflowEngine(msg2)
//                msg shouldBe msg2
//                avroMsg shouldBe avroMsg2
//            }
//        }
//    }
// }
