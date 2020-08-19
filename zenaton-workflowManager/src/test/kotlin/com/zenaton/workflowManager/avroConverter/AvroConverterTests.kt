package com.zenaton.workflowManager.avroConverter

import com.zenaton.workflowManager.messages.ForWorkflowEngineMessage
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.shouldSpec
import io.kotest.matchers.shouldBe

class AvroConverterTests : ShouldSpec({
//    context("WorkflowStateEngine") {
//        should("should be avro-reversible") {
//            val o1 = TestFactory.random(WorkflowEngineState::class)
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
//    context("StepCriterion") {
//        TestFactory.stepCriteria().forEach {
//            val description = it.key
//            val o1 = it.value
//            should("$description should be avro-convertible") {
//                val o2 = AvroConverter.toAvroStepCriterion(o1)
//                val o3 = AvroConverter.fromAvroStepCriterion(o2)
//                val o4 = AvroConverter.toAvroStepCriterion(o3)
//                o1 shouldBe o3
//                o2 shouldBe o4
//            }
//        }
//    }
//
//    ForWorkflowEngineMessage::class.sealedSubclasses.forEach {
//        val msg = TestFactory.random(it)
//        include(messagesToWorkflowEngineShouldBeAvroReversible(msg))
//    }
})

fun messagesToWorkflowEngineShouldBeAvroReversible(msg: ForWorkflowEngineMessage) = shouldSpec {
    context(msg::class.simpleName!!) {
        should("should be avro-convertible") {
            shouldNotThrowAny {
                val avroMsg = AvroConverter.toWorkflowEngine(msg)
                val msg2 = AvroConverter.fromWorkflowEngine(avroMsg)
                val avroMsg2 = AvroConverter.toWorkflowEngine(msg2)
                msg shouldBe msg2
                avroMsg shouldBe avroMsg2
            }
        }
    }
}
