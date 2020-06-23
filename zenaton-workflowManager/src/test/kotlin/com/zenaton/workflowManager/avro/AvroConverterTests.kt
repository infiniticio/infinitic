package com.zenaton.workflowManager.avro

import com.zenaton.jobManager.messages.Message
import com.zenaton.workflowManager.data.DecisionInput
import com.zenaton.workflowManager.data.actions.Action
import com.zenaton.workflowManager.data.actions.AvroAction
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.state.getStepId
import com.zenaton.workflowManager.data.steps.StepCriterion
import com.zenaton.workflowManager.engine.WorkflowEngineState
import com.zenaton.workflowManager.messages.envelopes.ForDecisionEngineMessage
import com.zenaton.workflowManager.messages.envelopes.ForTaskEngineMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import com.zenaton.workflowManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.shouldSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

fun getStepId() = TestFactory.get(StepCriterion.Id::class)

class AvroConverterTests : ShouldSpec({
    context("WorkflowStateEngine") {
        should("should all be avro-reversible") {
            val o1 = TestFactory.get(WorkflowEngineState::class)
            val o2 = AvroConverter.toStorage(o1)
            val o3 = AvroConverter.fromStorage(o2)
            o1 shouldBe o3
        }
    }

    context("DecisionInput") {
        should("should all be avro-reversible") {
            val o1 = TestFactory.get(DecisionInput::class)
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
                    val o1 = TestFactory.get(klass)
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
            val o1 = TestFactory.get(Branch::class)
            val o2 = AvroConverter.toAvroBranch(o1)
            val o3 = AvroConverter.fromAvroBranch(o2)
            o1 shouldBe o3
        }
    }

    context("StepCriterion") {
        should("A should be avro reversible") {
            val step = getStepId()

            checkStepCriterionReversibility(step)
        }

        should("A AND B should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val step = StepCriterion.And(listOf(stepA, stepB))

            checkStepCriterionReversibility(step)
        }

        should("A OR B should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val step = StepCriterion.Or(listOf(stepA, stepB))

            checkStepCriterionReversibility(step)
        }

        should("A AND (B OR C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val stepC = getStepId()
            val step = StepCriterion.And(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }

        should("A OR (B AND C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val stepC = getStepId()
            val step = StepCriterion.Or(listOf(stepA, StepCriterion.And(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }

        should("A OR (B OR C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val stepC = getStepId()
            val step = StepCriterion.Or(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }

        should("A AND (B AND C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()

            val stepC = getStepId()
            val step = StepCriterion.Or(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }
    }

    Message::class.sealedSubclasses.forEach {
        val msg = TestFactory.get(it)
        if (msg is ForDecisionEngineMessage) {
            include(messagesToDecisionEngineShouldBeAvroConvertible(msg))
        }
        if (msg is ForTaskEngineMessage) {
            include(messagesToTaskEngineShouldBeAvroConvertible(msg))
        }
        if (msg is ForWorkflowEngineMessage) {
            include(messagesToWorkflowEngineShouldBeAvroReversible(msg))
        }
    }
})

fun messagesToDecisionEngineShouldBeAvroConvertible(msg: ForDecisionEngineMessage) = shouldSpec {
    context(msg::class.simpleName!!) {
        should("be avro-convertible") {
            shouldNotThrowAny {
                val avroMsg = AvroConverter.toDecisionEngine(msg)
            }
        }
    }
}

fun messagesToTaskEngineShouldBeAvroConvertible(msg: ForTaskEngineMessage) = shouldSpec {
    context(msg::class.simpleName!!) {
        should("be avro-convertible") {
            shouldNotThrowAny {
                val avroMsg = AvroConverter.toTaskEngine(msg)
            }
        }
    }
}

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

fun checkStepCriterionReversibility(o1: StepCriterion) {
    val o2 = AvroConverter.toAvroStepCriterion(o1)
    val o3 = AvroConverter.fromAvroStepCriterion(o2)
    o1 shouldBe o3
}
