package com.zenaton.workflowManager.avro

import com.zenaton.jobManager.messages.Message
import com.zenaton.workflowManager.data.DecisionInput
import com.zenaton.workflowManager.data.actions.Action
import com.zenaton.workflowManager.data.actions.AvroAction
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.engine.WorkflowEngineState
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import com.zenaton.workflowManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.shouldSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

class AvroConverterTests : ShouldSpec({
    context("WorkflowStateEngine") {
        should("should all be avro-reversible") {
            val o1 = TestFactory.random(WorkflowEngineState::class)
            val o2 = AvroConverter.toStorage(o1)
            val o3 = AvroConverter.fromStorage(o2)
            o1 shouldBe o3
        }
    }

    context("DecisionInput") {
        should("should all be avro-reversible") {
            val o1 = TestFactory.random(DecisionInput::class)
            val o2 = AvroConverter.toAvroDecisionInput(o1)
            val o3 = AvroConverter.fromAvroDecisionInput(o2)
            o1 shouldBe o3
        }
    }

    context("Actions") {
        should("should all be avro-reversible") {
            fun checkAction(klass: KClass<out Action>) {
                if (klass.isSealed) {
                    klass.sealedSubclasses.forEach { checkAction(it) }
                } else {
                    val o1 = TestFactory.random(klass)
                    val o2 = AvroConverter.convertJson<AvroAction>(o1)
                    val o3 = AvroConverter.convertJson<Action>(o2)
                    o1 shouldBe o3
                }
            }
            checkAction(Action::class)
        }
    }

    context("Branch") {
        should("should be avro-reversible") {
            val o1 = TestFactory.random(Branch::class)
            val o2 = AvroConverter.toAvroBranch(o1)
            val o3 = AvroConverter.fromAvroBranch(o2)
            o1 shouldBe o3
        }
    }

    context("StepCriterion") {
        TestFactory.stepCriteria().forEach {
            val description = it.key
            val o1 = it.value
            should("$description should be avro-convertible") {
                val o2 = AvroConverter.toAvroStepCriterion(o1)
                val o3 = AvroConverter.fromAvroStepCriterion(o2)
                o1 shouldBe o3
            }
        }
    }

    Message::class.sealedSubclasses.forEach {
        val msg = TestFactory.random(it)
        if (msg is ForWorkflowEngineMessage) {
            include(messagesToWorkflowEngineShouldBeAvroReversible(msg))
        }
    }
})

fun messagesToWorkflowEngineShouldBeAvroReversible(msg: ForWorkflowEngineMessage) = shouldSpec {
    context(msg::class.simpleName!!) {
        should("be avro-convertible") {
            shouldNotThrowAny {
                val avroMsg = AvroConverter.toWorkflowEngine(msg)
                val msg2 = AvroConverter.fromWorkflowEngine(avroMsg)
                msg shouldBe msg2
            }
        }
    }
}
